/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

export function isValidJSON(text: string) {
  try {
    JSON.parse(text);
    return true;
  } catch {
    return false;
  }
}

/**
 * @returns a filtered object containing only entries of the provided keys
 * @param {*} object
 * @param any[] keys
 */
export function pickFromObject(object: any, keys: any) {
  return Object.entries(object).reduce((result, [key, value]) => {
    return !keys.includes(key) ? result : {...result, [key]: value};
  }, {});
}

/**
 * immutable version of array[index] = updatedValue
 * @returns the original array with the provided updatedValue at the provided index
 * @param {any[]} array
 * @param {number} index
 * @param {any} updatedValue
 */
export function immutableArraySet(array: any, index: any, updatedValue: any) {
  return [...array.slice(0, index), updatedValue, ...array.slice(index + 1)];
}
