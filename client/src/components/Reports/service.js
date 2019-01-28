import {get, del, put, post} from 'request';
import {reportConfig, getDataKeys} from 'services';

const {isAllowed, getNext} = reportConfig;

export async function loadSingleReport(id) {
  const response = await get('api/report/' + id);

  return await response.json();
}

export async function remove(id) {
  return await del(`api/report/${id}?force=true`);
}

export async function loadProcessDefinitionXml(processDefinitionKey, processDefinitionVersion) {
  const response = await get('api/process-definition/xml', {
    processDefinitionKey,
    processDefinitionVersion
  });

  return await response.text();
}

export async function loadVariables(processDefinitionKey, processDefinitionVersion) {
  const response = await get('api/variables', {
    processDefinitionKey,
    processDefinitionVersion,
    namePrefix: '',
    sortOrder: 'asc',
    orderBy: 'name'
  });

  return await response.json();
}

export async function isSharingEnabled() {
  const response = await get(`api/share/isEnabled`);
  const json = await response.json();
  return json.enabled;
}

export async function evaluateReport(query) {
  let response;

  try {
    if (typeof query !== 'object') {
      // evaluate saved report
      response = await get(`api/report/${query}/evaluate`);
    } else {
      // evaluate unsaved report
      response = await post(`api/report/evaluate/`, query);
    }
  } catch (e) {
    return null;
  }

  return await response.json();
}

export async function saveReport(id, data, forceUpdate) {
  return await put(`api/report/${id}?force=${forceUpdate}`, data);
}

export async function shareReport(reportId) {
  const body = {
    reportId
  };
  const response = await post(`api/share/report`, body);

  const json = await response.json();
  return json.id;
}

export async function getSharedReport(reportId) {
  const response = await get(`api/share/report/${reportId}`);

  if (response.status > 201) {
    return '';
  } else {
    const json = await response.json();
    return json.id;
  }
}

export async function revokeReportSharing(id) {
  return await del(`api/share/report/${id}`);
}

export const isRawDataReport = (report, data) => {
  return (
    data &&
    data.view &&
    data.view.operation === 'rawData' &&
    report &&
    report.result &&
    report.result[0]
  );
};

export async function loadDecisionDefinitions() {
  const response = await get('api/decision-definition/groupedByKey');

  return await response.json();
}

export function isChecked(data, current) {
  return (
    current &&
    getDataKeys(data).every(
      prop =>
        JSON.stringify(current[prop]) === JSON.stringify(data[prop]) || Array.isArray(data[prop])
    )
  );
}

export function update(type, data, props) {
  switch (type) {
    case 'view':
      return updateView(data, props);
    case 'groupBy':
      return updateGroupBy(data, props);
    case 'visualization':
      return updateVisualization(data, props);
    default:
      throw new Error('Tried to update unknown property');
  }
}

function updateView(newView, props) {
  const changes = {view: {$set: newView}};

  if (newView.property !== 'duration' || newView.entity !== 'processInstance') {
    changes.parameters = {processPart: {$set: null}};
  }

  const newGroup = getNext(newView) || props.groupBy;
  // we need to compare the string representation for changes, because groupBy is an object, not a string
  if (newGroup && JSON.stringify(newGroup) !== JSON.stringify(props.groupBy)) {
    changes.groupBy = {$set: newGroup};
  }

  const newVisualization = getNext(newView, newGroup) || props.visualization;
  if (newVisualization && newVisualization !== props.visualization) {
    changes.visualization = {$set: newVisualization};
  }

  if (!isAllowed(newView, newGroup)) {
    changes.groupBy = {$set: null};
    changes.visualization = {$set: null};
  }

  if (!isAllowed(newView, newGroup, newVisualization)) {
    changes.visualization = {$set: null};
  }

  return props.updateReport(changes, true);
}

function updateGroupBy(newGroupBy, props) {
  const changes = {groupBy: {$set: newGroupBy}};

  const newVisualization = getNext(props.view, newGroupBy);

  if (newVisualization) {
    // if we have a predetermined next visualization, we set it
    changes.visualization = {$set: newVisualization};
  } else if (!isAllowed(props.view, newGroupBy, props.visualization)) {
    // if the current visualization is not valid anymore for the new group, we reset it
    changes.visualization = {$set: null};
  }

  return props.updateReport(changes, true);
}

function updateVisualization(newVisualization, props) {
  return props.updateReport({visualization: {$set: newVisualization}});
}
