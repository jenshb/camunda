import React from 'react';
import {shallow} from 'enzyme';

import {Dropdown} from 'components';

import ReportControlPanel from './ReportControlPanel';
import {
  extractDefinitionName,
  reportConfig,
  getFlowNodeNames,
  loadProcessDefinitionXml
} from 'services';

import * as service from './service';
import {DefinitionSelection} from 'components';

const flushPromises = () => new Promise(resolve => setImmediate(resolve));

jest.mock('services', () => {
  const rest = jest.requireActual('services');

  return {
    ...rest,
    loadProcessDefinitionXml: jest.fn().mockReturnValue('I am a process definition xml'),
    reportConfig: {
      process: {
        getLabelFor: () => 'foo',
        options: {
          view: {foo: {data: 'foo', label: 'viewfoo'}},
          groupBy: {
            foo: {data: 'foo', label: 'groupbyfoo'},
            variable: {data: {value: []}, label: 'Variables'}
          },
          visualization: {foo: {data: 'foo', label: 'visualizationfoo'}}
        },
        isAllowed: jest.fn().mockReturnValue(true),
        getNext: jest.fn(),
        update: jest.fn()
      }
    },
    extractDefinitionName: jest.fn(),
    formatters: {
      getHighlightedText: text => text
    },
    getFlowNodeNames: jest.fn().mockReturnValue({
      a: 'foo',
      b: 'bar'
    })
  };
});

service.loadVariables = jest.fn().mockReturnValue([]);

const report = {
  data: {
    processDefinitionKey: 'aKey',
    processDefinitionVersion: 'aVersion',
    view: {operation: 'count', entity: 'processInstance'},
    groupBy: {type: 'none', unit: null},
    visualization: 'number',
    filter: [],
    configuration: {xml: 'fooXml'},
    parameters: {}
  }
};

it('should call the provided updateReport property function when a setting changes', () => {
  const spy = jest.fn();
  const node = shallow(<ReportControlPanel report={report} updateReport={spy} />);

  node
    .find(Dropdown.Option)
    .at(0)
    .simulate('click');

  expect(spy).toHaveBeenCalled();
});

it('should disable the groupBy and visualization Selects if view is not selected', () => {
  const node = shallow(
    <ReportControlPanel report={{...report, data: {...report.data, view: ''}}} />
  );

  expect(node.find('.configDropdown').at(1)).toBeDisabled();
  expect(node.find('.configDropdown').at(2)).toBeDisabled();
});

it('should not disable the groupBy and visualization Selects if view is selected', () => {
  const node = shallow(<ReportControlPanel report={report} />);

  expect(node.find('.configDropdown').at(1)).not.toBeDisabled();
  expect(node.find('.configDropdown').at(2)).not.toBeDisabled();
});

it('should disable options, which would create wrong combination', () => {
  const spy = jest.fn();
  reportConfig.process.isAllowed.mockReturnValue(false);
  const node = shallow(<ReportControlPanel report={report} onChange={spy} />);
  node.setProps({view: 'baz'});

  expect(
    node
      .find('Dropdown')
      .at(1)
      .find('DropdownOption')
  ).toBeDisabled();
});

it('should show process definition name', async () => {
  extractDefinitionName.mockReturnValue('aName');

  const node = await shallow(<ReportControlPanel report={report} />);

  expect(node.find('.processDefinitionPopover').prop('title')).toContain('aName');
});

it('should change process definition name if process definition is updated', async () => {
  const node = await shallow(<ReportControlPanel report={report} />);

  extractDefinitionName.mockReturnValue('aName');
  node.setProps({processDefinitionKey: 'bar'});

  expect(node.find('.processDefinitionPopover').prop('title')).toContain('aName');
});

it('should load the variables of the process', () => {
  const node = shallow(<ReportControlPanel report={report} />);

  node.setProps({
    report: {
      ...report,
      data: {...report.data, processDefinitionKey: 'bar', processDefinitionVersion: 'ALL'}
    }
  });

  expect(service.loadVariables).toHaveBeenCalledWith('bar', 'ALL');
});

it('should include variables in the groupby options', () => {
  const node = shallow(<ReportControlPanel report={report} />);

  node.setState({variables: [{name: 'Var1'}, {name: 'Var2'}]});

  const varDropdown = node.find('[label="Group by"] Submenu DropdownOption');

  expect(varDropdown.at(0).prop('children')).toBe('Var1');
  expect(varDropdown.at(1).prop('children')).toBe('Var2');
});

it('should only include variables that match the typeahead', () => {
  const node = shallow(<ReportControlPanel report={report} />);

  node.setState({
    variables: [{name: 'Foo'}, {name: 'Bar'}, {name: 'Foobar'}],
    variableTypeaheadValue: 'foo'
  });

  const varDropdown = node.find('[label="Group by"] Submenu DropdownOption');

  expect(varDropdown).toHaveLength(2);
  expect(varDropdown.at(0).prop('children')).toBe('Foo');
  expect(varDropdown.at(1).prop('children')).toBe('Foobar');
});

it('should show pagination for many variables', () => {
  const node = shallow(<ReportControlPanel report={report} />);

  node.setState({
    variables: [
      {name: 'varA'},
      {name: 'varB'},
      {name: 'varC'},
      {name: 'varD'},
      {name: 'varE'},
      {name: 'varF'},
      {name: 'varG'}
    ]
  });

  const varDropdown = node.find('[label="Group by"] Submenu DropdownOption');

  expect(varDropdown).toHaveLength(5);
  expect(node.find('.loadMore')).toBePresent();
});

it('should not show an "Always show tooltips" button for other visualizations', () => {
  const node = shallow(<ReportControlPanel report={report} visualization="something" />);

  expect(node).not.toIncludeText('Always show tooltips');
});

it('should load the flownode names and hand them to the filter and process part', async () => {
  const node = shallow(
    <ReportControlPanel
      report={{
        ...report,
        data: {...report.data, view: {entity: 'processInstance', property: 'duration'}}
      }}
    />
  );

  await flushPromises();
  node.update();

  expect(getFlowNodeNames).toHaveBeenCalled();
  expect(node.find('Filter').prop('flowNodeNames')).toEqual(getFlowNodeNames());
  expect(node.find('ProcessPart').prop('flowNodeNames')).toEqual(getFlowNodeNames());
});

it('should only display process part button if view is process instance duration', () => {
  const node = shallow(
    <ReportControlPanel
      report={{
        ...report,
        data: {...report.data, view: {entity: 'processInstance', property: 'duration'}}
      }}
    />
  );

  expect(node.find('ProcessPart')).toBePresent();

  node.setProps({
    report: {
      ...report,
      data: {...report.data, view: {entity: 'processInstance', property: 'frequency'}}
    }
  });

  expect(node.find('ProcessPart')).not.toBePresent();
});

it('should only display target value button if view is flownode duration', () => {
  const node = shallow(
    <ReportControlPanel
      report={{
        ...report,
        data: {
          ...report.data,
          visualization: 'heat',
          view: {entity: 'flowNode', property: 'duration'}
        }
      }}
    />
  );

  expect(node.find('TargetValueComparison')).toBePresent();

  node.setProps({
    report: {...report, data: {...report.data, view: {entity: 'flowNode', property: 'frequency'}}}
  });

  expect(node.find('TargetValueComparison')).not.toBePresent();
});

it('should load the process definition xml when a new definition is selected', async () => {
  const spy = jest.fn();
  const node = shallow(<ReportControlPanel report={report} updateReport={spy} />);

  loadProcessDefinitionXml.mockClear();

  await node.find(DefinitionSelection).prop('onChange')('newDefinition', 1);

  expect(loadProcessDefinitionXml).toHaveBeenCalledWith('newDefinition', 1);
});

it('should remove incompatible filters when changing the process definition', async () => {
  const spy = jest.fn();
  const node = shallow(
    <ReportControlPanel
      report={{
        ...report,
        data: {
          ...report.data,
          filter: [{type: 'startDate'}, {type: 'executedFlowNodes'}, {type: 'variable'}]
        }
      }}
      updateReport={spy}
    />
  );

  await node.find(DefinitionSelection).prop('onChange')('newDefinition', 1);

  expect(spy.mock.calls[0][0].filter.$set).toEqual([{type: 'startDate'}]);
});

it('should reset the groupby and visualization when changing process definition and groupby is variable', async () => {
  const spy = jest.fn();
  const node = shallow(
    <ReportControlPanel
      report={{...report, data: {...report.data, groupBy: {type: 'variable'}}}}
      updateReport={spy}
    />
  );

  await node.find(DefinitionSelection).prop('onChange')('newDefinition', 1);

  expect(spy.mock.calls[0][0].groupBy).toEqual({$set: null});
  expect(spy.mock.calls[0][0].visualization).toEqual({$set: null});
});

it('should reset definition specific configurations on definition change', async () => {
  const spy = jest.fn();
  const node = shallow(<ReportControlPanel report={report} updateReport={spy} />);

  await node.find(DefinitionSelection).prop('onChange')('newDefinition', 1);

  expect(spy.mock.calls[0][0].configuration.excludedColumns).toBeDefined();
  expect(spy.mock.calls[0][0].configuration.columnOrder).toBeDefined();
  expect(spy.mock.calls[0][0].configuration.heatmapTargetValue).toBeDefined();
});
