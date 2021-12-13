/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React from 'react';
import {Link, Redirect} from 'react-router-dom';

import {
  Button,
  ShareEntity,
  ReportRenderer,
  Popover,
  Icon,
  Deleter,
  EntityName,
  InstanceCount,
  ReportDetails,
  DownloadButton,
} from 'components';
import {isSharingEnabled, isOptimizeCloudEnvironment} from 'config';
import {formatters, checkDeleteConflict} from 'services';
import {t} from 'translation';

import {shareReport, revokeReportSharing, getSharedReport} from './service';

import './ReportView.scss';

export default class ReportView extends React.Component {
  state = {
    deleting: null,
    sharingEnabled: false,
    isOptimizeCloud: true,
  };

  async componentDidMount() {
    this.setState({
      isOptimizeCloud: await isOptimizeCloudEnvironment(),
      sharingEnabled: await isSharingEnabled(),
    });
  }

  shouldShowCSVDownload = () => {
    const {report} = this.props;

    if (report.combined && typeof report.result !== 'undefined') {
      return true;
    }

    return this.props.report.result?.measures.length === 1;
  };

  constructCSVDownloadLink = () => {
    return `api/export/csv/${this.props.report.id}/${encodeURIComponent(
      formatters.formatFileName(this.props.report.name)
    )}.csv`;
  };

  render() {
    const {report, error} = this.props;
    const {redirect, sharingEnabled, isOptimizeCloud, deleting} = this.state;

    const {id, name, currentUserRole} = report;

    if (redirect) {
      return <Redirect to={redirect} />;
    }

    return (
      <div className="ReportView Report">
        <div className="Report__header">
          <div className="head">
            <EntityName details={<ReportDetails report={report} />}>{name}</EntityName>
            <div className="tools">
              {currentUserRole === 'editor' && (
                <>
                  <Link className="tool-button edit-button" to="edit">
                    <Button main tabIndex="-1">
                      <Icon type="edit" />
                      {t('common.edit')}
                    </Button>
                  </Link>
                  <Button
                    main
                    className="tool-button delete-button"
                    onClick={() => this.setState({deleting: {...report, entityType: 'report'}})}
                  >
                    <Icon type="delete" />
                    {t('common.delete')}
                  </Button>
                </>
              )}
              {!isOptimizeCloud && (
                <Popover
                  main
                  className="tool-button share-button"
                  icon="share"
                  title={t('common.sharing.buttonTitle')}
                  tooltip={!sharingEnabled ? t('common.sharing.disabled') : ''}
                  disabled={!sharingEnabled}
                >
                  <ShareEntity
                    type="report"
                    resourceId={id}
                    shareEntity={shareReport}
                    revokeEntitySharing={revokeReportSharing}
                    getSharedEntity={getSharedReport}
                  />
                </Popover>
              )}
              {this.shouldShowCSVDownload() && (
                <DownloadButton
                  main
                  href={this.constructCSVDownloadLink()}
                  totalCount={calculateTotalEntries(report)}
                >
                  <Icon type="save" />
                  {t('report.downloadCSV')}
                </DownloadButton>
              )}
            </div>
          </div>
          <InstanceCount report={report} />
        </div>
        <div className="Report__view">
          <div className="Report__content">
            <ReportRenderer error={error} report={report} loadReport={this.props.loadReport} />
          </div>
        </div>
        <Deleter
          type="report"
          entity={deleting}
          checkConflicts={({id}) => checkDeleteConflict(id, 'report')}
          onClose={() => this.setState({deleting: null})}
          onDelete={() => this.setState({redirect: '../../'})}
        />
      </div>
    );
  }
}

function calculateTotalEntries({data, result}) {
  if (data?.view?.properties[0] === 'rawData') {
    return result.instanceCount;
  }

  return result?.measures?.[0].data.length;
}
