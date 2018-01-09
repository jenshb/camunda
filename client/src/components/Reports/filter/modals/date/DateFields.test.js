import React from 'react';
import moment from 'moment';

import DateFields from './DateFields';
import {mount} from 'enzyme';

jest.mock('./DateRange', () => props => `DateRange: props: ${JSON.stringify(props)}`);
jest.mock('./DateInput', () => props => `DateInput: props: ${JSON.stringify(props)}`);

const format = 'YYYY-MM-DD';
const startDate = moment([2017, 8, 29]);
const endDate = moment([2020, 6, 5]);

it('should have start date input field', () => {
  const node = mount(<DateFields format={format} startDate={startDate} endDate={endDate} />);

  expect(node).toIncludeText('DateInput__start');
});

it('should have end date input field', () => {
  const node = mount(<DateFields format={format} startDate={startDate} endDate={endDate} />);

  expect(node).toIncludeText('DateInput__end');
});

it('should set startDate on date change of start date input field', () => {
  const spy = jest.fn();
  const node = mount(<DateFields format={format} startDate={startDate} endDate={endDate} onDateChange={spy} />);

  node.instance().setStartDate('change');

  expect(spy).toBeCalledWith('startDate', 'change');
});

it('should set endDate on date change of end date input field', () => {
  const spy = jest.fn();
  const node = mount(<DateFields format={format} startDate={startDate} endDate={endDate} onDateChange={spy} />);

  node.instance().setEndDate('change');

  expect(spy).toBeCalledWith('endDate', 'change');
});

it('should select date range popup on date input click', () => {
  const node = mount(<DateFields format={format} startDate={startDate} endDate={endDate} enableAddButton = {jest.fn()}/>);

  const evt = {nativeEvent: {stopImmediatePropagation: jest.fn()}};
  node.instance().toggleDateRangeForStart(evt);

  expect(evt.nativeEvent.stopImmediatePropagation).toHaveBeenCalled();
  expect(node.state('popupOpen')).toBe(true);
  expect(node.state('currentlySelectedField')).toBe('startDate');
});

it('should have DateRange', () => {
  const node = mount(<DateFields format={format} startDate={startDate} endDate={endDate} enableAddButton = {jest.fn()}/>);
  node.setState({popupOpen: true});

  expect(node).toIncludeText('DateRange');
});

it('should change currently selected date to endDate', () => {
  const spy = jest.fn();
  const node = mount(<DateFields format={format} startDate={startDate} endDate={endDate} onDateChange={spy} enableAddButton = {jest.fn()}/>);
  node.setState({popupOpen: true, currentlySelectedField: 'startDate'});

  node.instance().endDateField = document.createElement('input');
  node.instance().onDateRangeChange('whatever');

  expect(node.state('currentlySelectedField')).toBe('endDate');
});

it('should selected endDate after second selection', () => {
  const spy = jest.fn();
  const node = mount(<DateFields format={format} startDate={startDate} endDate={endDate} onDateChange={spy} enableAddButton = {jest.fn()}/>);
  node.setState({popupOpen: true, currentlySelectedField: 'startDate'});

  node.instance().endDateField = document.createElement('input');
  node.instance().onDateRangeChange('whatever');
  node.instance().onDateRangeChange('date2');

  expect(spy).toBeCalledWith('endDate', 'date2');
});
