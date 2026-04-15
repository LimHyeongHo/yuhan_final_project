import React from 'react';
import { Link } from 'react-router-dom';

const Header = () => {
  return (
    <header className="fixed top-0 w-full z-50 bg-white/80 backdrop-blur-xl border-b border-outline-variant/10 shadow-[0_4px_30px_rgba(0,0,0,0.02)]">
      <nav className="flex items-center justify-between px-8 h-20 w-full max-w-7xl mx-auto">
        <Link to="/" className="text-2xl font-bold tracking-tighter text-primary font-headline uppercase">대학생 공동주문 마켓</Link>
        <div className="hidden md:flex items-center space-x-8">
          <Link className="font-headline tracking-tight text-primary border-b-2 border-primary pb-1" to="/">Marketplace</Link>
          <Link className="font-headline tracking-tight text-on-surface-variant hover:text-primary transition-colors" to="/">How It Works</Link>
          <Link className="font-headline tracking-tight text-on-surface-variant hover:text-primary transition-colors" to="/">Community</Link>
        </div>
        <div className="flex items-center gap-4">
          <button className="material-symbols-outlined text-on-surface-variant hover:text-primary transition-colors">search</button>
          <Link to="/login" className="bg-primary text-on-primary px-6 py-2 rounded-lg font-bold hover:bg-primary/90 transition-all duration-300 active:scale-95 font-headline">Get Started</Link>
        </div>
      </nav>
    </header>
  );
};

export default Header;
