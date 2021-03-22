/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import pluralSuffix from 'modules/utils/pluralSuffix';

export function concatGroupTitle(
  workflowName: any,
  instancesCount: any,
  versionsCount: any
) {
  return `View ${pluralSuffix(instancesCount, 'Instance')} in ${pluralSuffix(
    versionsCount,
    'Version'
  )} of Workflow ${workflowName}`;
}

export function concatTitle(
  workflowName: any,
  instancesCount: any,
  versionName: any
) {
  return `View ${pluralSuffix(
    instancesCount,
    'Instance'
  )} in Version ${versionName} of Workflow ${workflowName}`;
}

export function concatGroupLabel(
  name: any,
  instancesCount: any,
  versionsCount: any
) {
  return `${name} – ${pluralSuffix(
    instancesCount,
    'Instance'
  )} in ${pluralSuffix(versionsCount, 'Version')}`;
}

export function concatLabel(name: any, instancesCount: any, version: any) {
  return `${name} – ${pluralSuffix(
    instancesCount,
    'Instance'
  )} in Version ${version}`;
}

export function concatButtonTitle(name: any, instancesCount: any) {
  return `Expand ${pluralSuffix(
    instancesCount,
    'Instance'
  )} of Workflow ${name}`;
}
