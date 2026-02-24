import React from 'react';
import { TrendingUp, Activity, Server, FileCheck } from 'lucide-react';

const StatCard = ({ title, value, subtitle, trend, isPositive }: any) => (
  <div className="bg-[#111111] p-6 rounded-2xl border border-[#2C2C2E] hover:border-[#00C805] transition-colors">
    <h3 className="text-[#8E8E93] text-sm font-medium mb-2">{title}</h3>
    <div className="flex items-end gap-3">
      <span className="text-3xl font-bold">{value}</span>
      {trend && (
        <span className={`text-sm font-medium mb-1 ${isPositive ? 'text-[#00C805]' : 'text-[#FF5000]'}`}>
          {isPositive ? '↑' : '↓'} {trend}
        </span>
      )}
    </div>
    <p className="text-[#8E8E93] text-sm mt-2">{subtitle}</p>
  </div>
);

export default function DashboardHome() {
  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <header>
        <h2 className="text-3xl md:text-4xl font-bold mb-2">Portfolio Overview</h2>
        <p className="text-[#8E8E93]">System status and recent backtest performance.</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard 
          title="Avg Strategy Return (YTD)" 
          value="+14.2%" 
          subtitle="Across 12 optimized strategies"
          trend="2.4%"
          isPositive={true}
        />
        <StatCard 
          title="Total Universe" 
          value="2,694" 
          subtitle="KOSPI, KOSDAQ, KONEX"
        />
        <StatCard 
          title="Data Verification" 
          value="93.7%" 
          subtitle="DART Financials Verified"
          trend="0.5%"
          isPositive={true}
        />
        <StatCard 
          title="System Status" 
          value="Healthy" 
          subtitle="All microservices online"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="bg-[#111111] p-6 rounded-2xl border border-[#2C2C2E]">
          <h3 className="text-xl font-bold mb-4 flex items-center gap-2">
            <TrendingUp className="text-[#00C805]" /> Top Strategies
          </h3>
          <div className="space-y-4">
            {/* Mock Data */}
            {[
              { name: 'Multi-Factor (Value+Mom)', cagr: '24.5%', mdd: '-12.4%' },
              { name: 'Dual Momentum', cagr: '18.2%', mdd: '-8.1%' },
              { name: 'Sector Rotation', cagr: '15.4%', mdd: '-15.2%' },
            ].map((s, i) => (
              <div key={i} className="flex justify-between items-center p-4 bg-black rounded-xl border border-[#2C2C2E]">
                <div>
                  <p className="font-bold">{s.name}</p>
                  <p className="text-sm text-[#8E8E93]">MDD: {s.mdd}</p>
                </div>
                <div className="text-right">
                  <p className="text-lg font-bold text-[#00C805]">{s.cagr}</p>
                  <p className="text-xs text-[#8E8E93]">CAGR</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-[#111111] p-6 rounded-2xl border border-[#2C2C2E]">
          <h3 className="text-xl font-bold mb-4 flex items-center gap-2">
            <Server className="text-[#8E8E93]" /> System Events
          </h3>
          <div className="space-y-4">
            <div className="flex gap-4 items-start">
              <div className="w-2 h-2 mt-2 rounded-full bg-[#00C805]"></div>
              <div>
                <p className="font-medium">Daily Price Batch Completed</p>
                <p className="text-sm text-[#8E8E93]">Today, 08:30 AM (2,832 symbols updated)</p>
              </div>
            </div>
            <div className="flex gap-4 items-start">
              <div className="w-2 h-2 mt-2 rounded-full bg-[#00C805]"></div>
              <div>
                <p className="font-medium">Finance Validation Sync</p>
                <p className="text-sm text-[#8E8E93]">Yesterday, 23:00 PM (Q3 Reports verified)</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
