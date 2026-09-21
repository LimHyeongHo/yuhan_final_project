import React from 'react';
import { Link } from 'react-router-dom';
import './SiteFooter.css';

const footerLinks = [
  { label: '유한대학교 공식 인스타그램', href: 'https://www.instagram.com/yuhan_univ/' },
  { label: '유한대 컴퓨터소프트웨어공학과', href: 'https://cse.yuhan.ac.kr/' },
];

const technologyStack = [
  'React',
  'Spring Boot',
  'Supabase',
  'WebSocket',
  'PKI',
  'Blockchain',
  'Web3j',
  'Toss Payments',
];

const PURCHASE_SAFETY_SERVICE_URL = 'https://consumer.tosspayments.com/escrow';

const openExternalPage = (url) => {
  window.open(url, '_blank', 'noopener,noreferrer');
};

const SiteFooter = () => (
  <footer className="site-footer">
    <div className="site-footer__container">
      <div className="site-footer__main">
        <section className="site-footer__brand-area" aria-labelledby="site-footer-title">
          <Link
            className="site-footer__brand"
            id="site-footer-title"
            to="/"
            onClick={() => window.scrollTo({ top: 0, left: 0, behavior: 'auto' })}
          >
            N-bbang
          </Link>
          <p className="site-footer__description">유한대학교 졸업작품 공동구매 플랫폼</p>
          <address className="site-footer__company-info">
            <span>(주)유한대학교</span>
            <span>경기도 부천시 경인로 590</span>
            <span>팀장 임형호</span>
            <span>팀원 홍준표, 안시연, 문건우</span>
            <span>대표번호 02-2610-0768</span>
          </address>

          <section className="site-footer__stack" aria-labelledby="site-footer-stack-title">
            <ul >
              {technologyStack.map((technology) => (
                <li key={technology}>{technology}</li>
              ))}
            </ul>
          </section>
        </section>

        <div className="site-footer__information">
          <nav className="site-footer__nav" aria-label="하단 메뉴">
            <ul className="site-footer__link-list">
              {footerLinks.map((link) => (
                <li key={link.label}>
                  <button type="button" onClick={() => openExternalPage(link.href)}>
                    {link.label}
                  </button>
                </li>
              ))}
            </ul>
          </nav>

          <section className="site-footer__safety-service" aria-labelledby="site-footer-safety-title">
            <div className="site-footer__safety-heading">
              <h2 id="site-footer-safety-title">토스페이먼츠 구매안전서비스</h2>
              <button type="button" onClick={() => openExternalPage(PURCHASE_SAFETY_SERVICE_URL)}>
                구매안전서비스
              </button>
            </div>
            <p>
              고객님은 안전거래를 위해 현금 등으로 결제시 저희 쇼핑몰에서 가입한 토스페이먼츠의
              구매안전서비스를 이용하실 수 있습니다.
            </p>
          </section>
        </div>
      </div>

      <div className="site-footer__bottom">
        <small>© {new Date().getFullYear()} N-bbang. All rights reserved.</small>
      </div>
    </div>
  </footer>
);

export default SiteFooter;
