import React from 'react';
import Hero from './Hero';
import RoleSection from './RoleSection';
import BenefitsSection from './BenefitsSection';
import CTASection from './CTASection';

const Home = () => {
  return (
    <main className="pt-20">
      <Hero />
      <RoleSection />
      <BenefitsSection />
      <CTASection />
    </main>
  );
};

export default Home;
