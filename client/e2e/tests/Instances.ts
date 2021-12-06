/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {config} from '../config';
import {
  setup,
  cmRunningInstancesCheckbox,
  cmActiveCheckbox,
  cmIncidentsCheckbox,
  cmFinishedInstancesCheckbox,
  cmCompletedCheckbox,
  cmCanceledCheckbox,
} from './Instances.setup';
import {deploy} from '../setup-utils';
import {demoUser} from './utils/Roles';
import {wait} from './utils/wait';
import {getPathname} from './utils/getPathname';
import {getSearch} from './utils/getSearch';
import {convertToQueryString} from './utils/convertToQueryString';
import {screen, within} from '@testing-library/testcafe';
import {IS_NEW_FILTERS_FORM} from '../../src/modules/feature-flags';
import {setFlyoutTestAttribute} from './utils/setFlyoutTestAttribute';

fixture('Instances')
  .page(config.endpoint)
  .before(async (ctx) => {
    ctx.initialData = await setup();
    await wait();
  })
  .beforeEach(async (t) => {
    await t.useRole(demoUser).click(
      screen.queryByRole('link', {
        name: /view instances/i,
      })
    );

    if (IS_NEW_FILTERS_FORM) {
      await setFlyoutTestAttribute('processName');
    }
  });

test('Instances Page Initial Load', async (t) => {
  const {initialData} = t.fixtureCtx;

  await t.click(
    screen.getByRole('link', {
      name: /view instances/i,
    })
  );

  if (IS_NEW_FILTERS_FORM) {
    await t
      .expect(cmRunningInstancesCheckbox.hasClass('checked'))
      .ok()
      .expect(cmActiveCheckbox.hasClass('checked'))
      .ok()
      .expect(cmIncidentsCheckbox.hasClass('checked'))
      .ok()
      .expect(cmFinishedInstancesCheckbox.hasClass('checked'))
      .notOk()
      .expect(cmCompletedCheckbox.hasClass('checked'))
      .notOk()
      .expect(cmCanceledCheckbox.hasClass('checked'))
      .notOk();
  } else {
    await t
      .expect(
        screen.queryByRole('checkbox', {name: 'Running Instances'}).checked
      )
      .ok()
      .expect(screen.queryByRole('checkbox', {name: 'Active'}).checked)
      .ok()
      .expect(screen.queryByRole('checkbox', {name: 'Incidents'}).checked)
      .ok()
      .expect(
        screen.queryByRole('checkbox', {name: 'Finished Instances'}).checked
      )
      .notOk()
      .expect(screen.queryByRole('checkbox', {name: 'Completed'}).checked)
      .notOk()
      .expect(screen.queryByRole('checkbox', {name: 'Canceled'}).checked)
      .notOk();
  }

  await t
    .expect(screen.queryByText('There is no Process selected').exists)
    .ok()
    .expect(
      screen.queryByText(
        'To see a Diagram, select a Process in the Filters panel'
      ).exists
    )
    .ok();

  await t.typeText(
    screen.queryByRole('textbox', {
      name: /instance id\(s\) separated by space or comma/i,
    }),
    `${initialData.instanceWithoutAnIncident.processInstanceKey}, ${initialData.instanceWithAnIncident.processInstanceKey}`
  );

  await t.expect(screen.queryByTestId('instances-list').exists).ok();

  const withinInstancesList = within(screen.queryByTestId('instances-list'));
  await t.expect(withinInstancesList.getAllByRole('row').count).eql(2);

  await t
    .expect(
      withinInstancesList.queryByTestId(
        `INCIDENT-icon-${initialData.instanceWithAnIncident.processInstanceKey}`
      ).exists
    )
    .ok()
    .expect(
      withinInstancesList.queryByTestId(
        `ACTIVE-icon-${initialData.instanceWithoutAnIncident.processInstanceKey}`
      ).exists
    )
    .ok();
});

test('Select flow node in diagram', async (t) => {
  const {initialData} = t.fixtureCtx;
  const instance = initialData.instanceWithoutAnIncident;

  await t.click(
    screen.getByRole('link', {
      name: /view instances/i,
    })
  );

  // Filter by Instance ID
  await t.typeText(
    screen.queryByRole('textbox', {
      name: 'Instance Id(s) separated by space or comma',
    }),
    instance.processInstanceKey,
    {paste: true}
  );

  const processCombobox = IS_NEW_FILTERS_FORM
    ? screen.queryByTestId('filter-process-name')
    : screen.queryByRole('combobox', {
        name: 'Process',
      });

  // Select "Order Process"
  await t
    .click(processCombobox)
    .click(
      IS_NEW_FILTERS_FORM
        ? within(
            screen.queryByTestId('cm-flyout-process-name').shadowRoot()
          ).queryByText('Order process')
        : within(processCombobox).queryByRole('option', {
            name: 'Order process',
          })
    )
    .expect(screen.queryByTestId('diagram').exists)
    .ok();

  // Select "Ship Articles" flow node
  const shipArticlesTaskId = 'shipArticles';

  await t.click(
    within(screen.queryByTestId('diagram')).queryByText('Ship Articles')
  );

  if (IS_NEW_FILTERS_FORM) {
    await t
      .expect(
        within(
          screen.queryByTestId('filter-flow-node').shadowRoot()
        ).queryByText('Ship Articles').exists
      )
      .ok();
  } else {
    await t
      .expect(screen.queryByRole('combobox', {name: 'Flow Node'}).value)
      .eql(shipArticlesTaskId);
  }

  await t
    .expect(
      screen.queryByText('There are no Instances matching this filter set')
        .exists
    )
    .ok()
    .expect(await getPathname())
    .eql('/instances')
    .expect(await getSearch())
    .eql(
      convertToQueryString({
        active: 'true',
        incidents: 'true',
        ids: instance.processInstanceKey,
        process: 'orderProcess',
        version: '1',
        flowNodeId: shipArticlesTaskId,
      })
    );

  // Select "Check Payment" flow node
  const checkPaymentTaskId = 'checkPayment';
  await t.click(
    within(screen.queryByTestId('diagram')).queryByText('Check payment')
  );

  if (IS_NEW_FILTERS_FORM) {
    await t
      .expect(
        within(
          screen.queryByTestId('filter-flow-node').shadowRoot()
        ).queryByText('Check payment').exists
      )
      .ok();
  } else {
    await t
      .expect(screen.queryByRole('combobox', {name: 'Flow Node'}).value)
      .eql(checkPaymentTaskId);
  }

  await t
    .expect(
      within(screen.queryByTestId('instances-list')).getAllByRole('row').count
    )
    .eql(1)
    .expect(await getPathname())
    .eql('/instances')
    .expect(await getSearch())
    .eql(
      convertToQueryString({
        active: 'true',
        incidents: 'true',
        ids: instance.processInstanceKey,
        process: 'orderProcess',
        version: '1',
        flowNodeId: checkPaymentTaskId,
      })
    );
});

test('Wait for process creation', async (t) => {
  await t.navigateTo(
    '/instances?active=true&incidents=true&process=testProcess&version=1'
  );

  await t.expect(screen.queryByTestId('listpanel-skeleton').exists).ok();
  await t.expect(screen.queryByTestId('diagram-spinner').exists).ok();

  if (IS_NEW_FILTERS_FORM) {
    await t
      .expect(
        screen.queryByTestId('filter-process-name').getAttribute('disabled')
      )
      .eql('true');
  } else {
    await t
      .expect(
        screen
          .queryByRole('combobox', {
            name: 'Process',
          })
          .hasAttribute('disabled')
      )
      .ok();
  }

  await deploy(['./e2e/tests/resources/newProcess.bpmn']);

  await t.expect(screen.queryByTestId('diagram').exists).ok();
  await t.expect(screen.queryByTestId('diagram-spinner').exists).notOk();

  await t.expect(screen.queryByTestId('listpanel-skeleton').exists).notOk();
  await t
    .expect(
      screen.getByText('There are no Instances matching this filter set').exists
    )
    .ok();

  if (IS_NEW_FILTERS_FORM) {
    await t
      .expect(
        screen.getByTestId('filter-process-name').getAttribute('disabled')
      )
      .eql('false');
    await t
      .expect(
        within(
          screen.getByTestId('filter-process-name').shadowRoot()
        ).getByText('Test Process').exists
      )
      .ok();
  } else {
    await t
      .expect(
        screen
          .getByRole('combobox', {
            name: 'Process',
          })
          .hasAttribute('disabled')
      )
      .notOk();
    await t
      .expect(
        screen.getByRole('combobox', {
          name: 'Process',
        }).value
      )
      .eql('testProcess');
  }
});
