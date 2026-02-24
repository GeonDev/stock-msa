import React, { useState } from 'react';
import { Play, Settings2 } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

const mockChartData = Array.from({ length: 12 }, (_, i) => ({
  month: `2023-${String(i + 1).padStart(2, '0')}`,
  portfolio: 10000000 * Math.pow(1.02, i) * (1 + (Math.random() * 0.05 - 0.025)),
  benchmark: 10000000 * Math.pow(1.01, i) * (1 + (Math.random() * 0.06 - 0.03)),
}));

export default function BacktestSimulator() {
  const [isRunning, setIsRunning] = useState(false);

  const handleRun = () => {
    setIsRunning(true);
    setTimeout(() => setIsRunning(false), 2000);
  };

  return (
    <div className="flex flex-col lg:flex-row gap-8 h-[calc(100vh-8rem)]">
      
      {/* Settings Panel */}
      <div className="w-full lg:w-80 bg-[#111111] p-6 rounded-2xl border border-[#2C2C2E] flex flex-col gap-6 overflow-y-auto">
        <div className="flex items-center gap-2">
          <Settings2 className="text-[#8E8E93]" />
          <h2 className="text-xl font-bold">Parameters</h2>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm text-[#8E8E93] mb-2">Strategy Type</label>
            <select className="w-full bg-black border border-[#2C2C2E] rounded-xl p-3 text-white focus:outline-none focus:border-[#00C805]">
              <option>Value (PER/PBR)</option>
              <option>Momentum</option>
              <option>Multi-Factor</option>
              <option>Sector Rotation</option>
              <option>Dual Momentum</option>
            </select>
          </div>

          <div>
            <label className="block text-sm text-[#8E8E93] mb-2">Initial Capital (₩)</label>
            <input type="text" defaultValue="10,000,000" className="w-full bg-black border border-[#2C2C2E] rounded-xl p-3 text-white focus:outline-none focus:border-[#00C805]" />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-[#8E8E93] mb-2">Start Date</label>
              <input type="date" defaultValue="2023-01-01" className="w-full bg-black border border-[#2C2C2E] rounded-xl p-3 text-white focus:outline-none focus:border-[#00C805]" />
            </div>
            <div>
              <label className="block text-sm text-[#8E8E93] mb-2">End Date</label>
              <input type="date" defaultValue="2023-12-31" className="w-full bg-black border border-[#2C2C2E] rounded-xl p-3 text-white focus:outline-none focus:border-[#00C805]" />
            </div>
          </div>

          <div>
            <label className="block text-sm text-[#8E8E93] mb-2">Slippage Model</label>
            <select className="w-full bg-black border border-[#2C2C2E] rounded-xl p-3 text-white focus:outline-none focus:border-[#00C805]">
              <option>Fixed (0.2%)</option>
              <option>Volume-Based</option>
              <option>None</option>
            </select>
          </div>
        </div>

        <button 
          onClick={handleRun}
          disabled={isRunning}
          className="mt-auto w-full bg-[#00C805] hover:bg-[#00E605] text-black font-bold py-4 rounded-xl flex justify-center items-center gap-2 transition-colors disabled:opacity-50"
        >
          {isRunning ? <span className="animate-spin">↻</span> : <Play size={20} />}
          {isRunning ? 'Running Simulation...' : 'Run Backtest'}
        </button>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col gap-6 overflow-y-auto">
        
        {/* KPI Hero Section */}
        <div className="flex flex-wrap gap-8 items-end pb-6 border-b border-[#2C2C2E]">
          <div>
            <p className="text-[#8E8E93] text-sm font-medium mb-1">Final Portfolio Value</p>
            <h1 className="text-5xl font-bold text-white tracking-tight">₩ 12,450,200</h1>
          </div>
          <div>
            <p className="text-[#8E8E93] text-sm font-medium mb-1">CAGR</p>
            <h2 className="text-3xl font-bold text-[#00C805]">+24.50%</h2>
          </div>
          <div>
            <p className="text-[#8E8E93] text-sm font-medium mb-1">MDD</p>
            <h2 className="text-3xl font-bold text-[#FF5000]">-12.40%</h2>
          </div>
          <div>
            <p className="text-[#8E8E93] text-sm font-medium mb-1">Sharpe</p>
            <h2 className="text-3xl font-bold text-white">1.85</h2>
          </div>
        </div>

        {/* Chart Area */}
        <div className="flex-1 min-h-[400px] bg-black rounded-2xl">
           <ResponsiveContainer width="100%" height="100%">
            <LineChart data={mockChartData} margin={{ top: 20, right: 20, left: 20, bottom: 20 }}>
              <Tooltip 
                contentStyle={{ backgroundColor: '#111111', borderColor: '#2C2C2E', borderRadius: '12px' }}
                itemStyle={{ color: '#fff' }}
              />
              <Line 
                type="monotone" 
                dataKey="portfolio" 
                stroke="#00C805" 
                strokeWidth={3} 
                dot={false}
                activeDot={{ r: 6, fill: '#00C805' }}
              />
              <Line 
                type="monotone" 
                dataKey="benchmark" 
                stroke="#8E8E93" 
                strokeWidth={2} 
                strokeDasharray="5 5"
                dot={false} 
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

      </div>
    </div>
  );
}
