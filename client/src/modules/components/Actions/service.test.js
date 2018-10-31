import {
  INSTANCE_STATE,
  OPERATION_TYPE,
  OPERATION_STATE
} from 'modules/constants';
import {
  wrapIdinQuery,
  isWithIncident,
  isRunning,
  getLatestOperation,
  getLatestOperationState
} from './service';

import {xTimes, createOperation} from 'modules/testUtils';

const mockIncidentInstance = {
  id: '8590375632-2',
  state: 'INCIDENT',
  incidents: [{state: 'ACTIVE'}]
};

const mockActiveInstance = {
  id: '8590375632-2',
  state: 'ACTIVE'
};

const mockIncidentQuery = [
  {ids: ['8590375632-2'], incidents: true, running: true}
];

const mockActiveQuery = [{ids: ['8590375632-2'], active: true, running: true}];

describe('Action services', () => {
  describe('Action Buttons', () => {
    describe('isWithIncident', () => {
      it('should return true if an instance has an incident', () => {
        expect(isWithIncident(mockIncidentInstance)).toBe(true);
      });

      it('should return false if an instance is active', () => {
        expect(isWithIncident(mockActiveInstance)).toBe(false);
      });
    });

    describe('wrapIdinQuery', () => {
      it('should return cancel-query for active instance', () => {
        expect(
          wrapIdinQuery(OPERATION_TYPE.CANCEL, mockActiveInstance)
        ).toEqual(mockActiveQuery);
      });

      it('should return cancel-query for instance with incident ', () => {
        expect(
          wrapIdinQuery(OPERATION_TYPE.CANCEL, mockIncidentInstance)
        ).toEqual(mockIncidentQuery);
      });

      it('should return retry-query for instance with incidents', () => {
        expect(
          wrapIdinQuery(OPERATION_TYPE.UPDATE_RETRIES, mockIncidentInstance)
        ).toEqual(mockIncidentQuery);
      });
    });

    describe('isRunning', () => {
      const mockCompletedInstance = {
        id: '8590375632-2',
        state: INSTANCE_STATE.COMPLETED
      };
      const mockCanceldInstance = {
        id: '8590375632-2',
        state: INSTANCE_STATE.CANCELED
      };

      it('should return true if an instance is running', () => {
        expect(isRunning(mockIncidentInstance)).toBe(true);
        expect(isRunning(mockActiveInstance)).toBe(true);

        expect(isRunning(mockCompletedInstance)).toBe(false);
        expect(isRunning(mockCanceldInstance)).toBe(false);
      });
    });
  });

  describe('Action Status', () => {
    let mockOperations;

    const createMockOperations = (amount, array) =>
      xTimes(amount)(counter => {
        array.push(
          createOperation({startDate: `2018-10-1${counter}T09:20:38.661Z`})
        );
      });

    describe('getLatestOperation', () => {
      it('should return "" when no operations are available', () => {
        mockOperations = [];
        createMockOperations(0, mockOperations);
        expect(getLatestOperation(mockOperations)).toEqual('');
      });

      it('should retrun operations sorted in ascending order by startDate', () => {
        // Create Mock Data
        mockOperations = [];
        createMockOperations(3, mockOperations);
        const latestDate = `2018-10-13T09:20:38.661Z`;

        expect(getLatestOperation(mockOperations)).toEqual(
          createOperation({startDate: latestDate})
        );
      });
    });
  });
});
