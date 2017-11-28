import React from 'react';
import {Link} from 'react-router-dom';

import HeaderNav from './HeaderNav';
import LogoutButton from './LogoutButton';

import {getToken} from 'credentials';

import './Header.css';

export default function Header({name}) {
  return (
    <header role='banner' className='Header'>
      <Link to='/' className='Header__link' title={name}>
        <span className='Header__brand-logo' />
        <span>{name}</span>
      </Link>
      {(getToken() &&
        <HeaderNav>
          <HeaderNav.Item name='Dashboards' linksTo='/dashboards' active='/dashboard' />
          <HeaderNav.Item name='Reports' linksTo='/reports' active='/report' />
        </HeaderNav>
      )}
      {(getToken() &&
        <LogoutButton />
      )}
    </header>
  );
}
