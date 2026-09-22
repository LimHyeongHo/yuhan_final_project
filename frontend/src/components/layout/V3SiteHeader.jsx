import React from 'react';
import { IntegratedHeader } from '../../pages/home/homePage_v3';
import './V3SiteHeader.css';

const V3SiteHeader = ({ embedded = false }) => {
  if (embedded) return <IntegratedHeader />;

  return (
    <div className="v3-site-header-shell v3-design-header-shell">
      <div className="v3-design-container">
        <IntegratedHeader />
      </div>
    </div>
  );
};

export default V3SiteHeader;
