import React from 'react';
import './HeaderLogo.css';

const logoUrl = `${process.env.PUBLIC_URL}/logo/00. N-bbang_icon_logo.png`;

const HeaderLogo = () => (
  <span
    aria-hidden="true"
    className="brand-logo-image"
    style={{ backgroundImage: `url("${logoUrl}")` }}
  />
);

export default HeaderLogo;
