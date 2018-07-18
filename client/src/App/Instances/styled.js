import styled from 'styled-components';

import SplitPane from 'modules/components/SplitPane';
import Panel from 'modules/components/Panel';
import ExpandButton from 'modules/components/ExpandButton';

import {HEADER_HEIGHT} from './../Header/styled';

export const Instances = styled.div`
  display: flex;
  flex-direction: row;
  height: calc(100vh - ${HEADER_HEIGHT}px);
`;

export const Filters = styled.div`
  display: flex;
  width: 320px;
  margin-right: 1px;
  flex-shrink: 0;
`;

export const Center = styled(SplitPane)`
  width: 100%;
`;

export const Selections = styled.div`
  width: 479px;
  display: flex;

  margin-left: 1px;
`;

export const RightExpandButton = styled(ExpandButton)`
  position: absolute;
  left: 0;
  top: 0;
  border-top: none;
  border-bottom: none;
  border-left: none;
`;

export const SelectionHeader = styled(Panel.Header)`
  padding-left: 45px;
  display: flex;
  flex-shrink: 0;
`;
