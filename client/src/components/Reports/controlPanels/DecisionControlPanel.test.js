import React from 'react';
import {shallow} from 'enzyme';

import {Dropdown, Popover, DefinitionSelection} from 'components';
import {extractDefinitionName} from 'services';

import DecisionControlPanel from './DecisionControlPanel';

jest.mock('services', () => {
  const rest = jest.requireActual('services');

  return {
    ...rest,
    loadDecisionDefinitionXml: jest.fn().mockReturnValue('somexml'),
    extractDefinitionName: jest.fn(),
    reportConfig: {
      ...rest.reportConfig,
      decision: {
        getLabelFor: () => 'foo',
        options: {
          view: {foo: {data: 'foo', label: 'viewfoo'}},
          groupBy: {
            foo: {data: 'foo', label: 'groupbyfoo'},
            inputVariable: {data: {value: []}, label: 'Input Variable'}
          },
          visualization: {foo: {data: 'foo', label: 'visualizationfoo'}}
        },
        isAllowed: jest.fn().mockReturnValue(true),
        getNext: jest.fn(),
        update: jest.fn()
      }
    }
  };
});

const report = {
  data: {
    decisionDefinitionKey: 'aKey',
    decisionDefinitionVersion: 'aVersion',
    view: {operation: 'rawData'},
    groupBy: {type: 'none', unit: null},
    visualization: 'table',
    filter: [],
    configuration: {
      xml:
        '<decision id="aKey"><input id="anId" label="aName"><inputExpression typeRef="string" /></input><input id="anotherId" label="anotherName"><inputExpression typeRef="string" /></input></decision>'
    }
  }
};

it('should call the provided updateReport property function when a setting changes', () => {
  const spy = jest.fn();
  const node = shallow(<DecisionControlPanel report={report} updateReport={spy} />);

  node
    .find(Dropdown.Option)
    .at(0)
    .simulate('click');

  expect(spy).toHaveBeenCalled();
});

it('should disable the groupBy and visualization Selects if view is not selected', () => {
  const node = shallow(
    <DecisionControlPanel report={{...report, data: {...report.data, view: ''}}} />
  );

  expect(node.find('.configDropdown').at(1)).toBeDisabled();
  expect(node.find('.configDropdown').at(2)).toBeDisabled();
});

it('should not disable the groupBy and visualization Selects if view is selected', () => {
  const node = shallow(<DecisionControlPanel report={report} />);

  expect(node.find('.configDropdown').at(1)).not.toBeDisabled();
  expect(node.find('.configDropdown').at(2)).not.toBeDisabled();
});

it('should include variables in the groupby options', () => {
  const node = shallow(<DecisionControlPanel report={report} />);

  const varDropdown = node.find('[label="Group by"] Submenu DropdownOption');

  expect(varDropdown.at(0).prop('children')).toBe('aName');
  expect(varDropdown.at(1).prop('children')).toBe('anotherName');
});

it('should parse variables from the xml', () => {
  const node = shallow(<DecisionControlPanel report={report} />);

  expect(node.state().variables.inputVariable).toEqual([
    {id: 'anId', name: 'aName', type: 'string'},
    {id: 'anotherId', name: 'anotherName', type: 'string'}
  ]);
});

it('should reset variable groupby on definition change', async () => {
  const spy = jest.fn();
  const node = shallow(
    <DecisionControlPanel
      report={{
        data: {
          ...report.data,
          groupBy: {type: 'inputVariable', value: {id: 'clause1', name: 'Invoice Amount'}}
        }
      }}
      updateReport={spy}
    />
  );

  await node.find(DefinitionSelection).prop('onChange')('newDefinition', '1');

  expect(spy).toHaveBeenCalled();
  expect(spy.mock.calls[0][0].groupBy).toEqual({$set: null});
});

it('should reset variable filters on definition change', async () => {
  const spy = jest.fn();
  const node = shallow(
    <DecisionControlPanel
      report={{
        data: {
          ...report.data,
          filter: [{type: 'inputVariable'}, {type: 'evaluationDateTime'}, {type: 'outputVariable'}]
        }
      }}
      updateReport={spy}
    />
  );

  await node.find(DefinitionSelection).prop('onChange')('newDefinition', '1');

  expect(spy).toHaveBeenCalled();
  expect(spy.mock.calls[0][0].filter).toEqual({$set: [{type: 'evaluationDateTime'}]});
});

it('should reset definition specific configurations on definition change', async () => {
  const spy = jest.fn();
  const node = shallow(<DecisionControlPanel report={report} updateReport={spy} />);

  await node.find(DefinitionSelection).prop('onChange')('newDefinition', '1');

  expect(spy.mock.calls[0][0].configuration.excludedColumns).toBeDefined();
  expect(spy.mock.calls[0][0].configuration.columnOrder).toBeDefined();
});

it('should not crash when no decisionDefinition is selected', () => {
  shallow(
    <DecisionControlPanel
      report={{
        data: {...report.data, decisionDefinitionKey: null, decisionDefinitionVersion: null}
      }}
    />
  );
});

it('should show decision definition name', async () => {
  extractDefinitionName.mockReturnValue('aName');

  const node = await shallow(<DecisionControlPanel report={report} />);

  expect(node.find(Popover).prop('title')).toContain('aName');
});

it('should change decision definition name if decision definition is updated', async () => {
  extractDefinitionName.mockReturnValue('aName');
  const node = await shallow(<DecisionControlPanel report={report} />);

  extractDefinitionName.mockReturnValue('anotherName');
  node.setProps({decisionDefinitionKey: 'bar'});

  expect(node.find(Popover).prop('title')).toContain('anotherName');
});
