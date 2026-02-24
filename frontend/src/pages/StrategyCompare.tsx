import React from 'react';
import { Layers } from 'lucide-react';
import { LineChart, Line, XAxis, Tooltip, ResponsiveContainer } from 'recharts';

const mockCompareData = Array.from({ length: 12 }, (_, i) => ({
  month: `2023-${String(i + 1).padStart(2, '0')}`,
  strategyA: 10000 * Math.pow(1.02, i) * (1 + (Math.random() * 0.05 - 0.025)),
  strategyB: 10000 * Math.pow(1.015, i) * (1 + (Math.random() * 0.04 - 0.02)),
  strategyC: 10000 * Math.pow(1.03, i) * (1 + (Math.random() * 0.08 - 0.04)),
}));

export default function StrategyCompare() {
  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <header className="flex justify-between items-end">
        <div>
          <h2 className="text-3xl md:text-4xl font-bold mb-2">Strategy Compare</h2>
          <p className="text-[#8E8E93]">Compare multiple backtest results and grid search optimization rankings.</p>
        </div>
        <button className="bg-[#111111] border border-[#2C2C2E] hover:border-[#00C805] px-4 py-2 rounded-xl flex items-center gap-2 transition-colors">
          <Layers size={18} />
          <span>Add Strategy</span>
        </button>
      </header>

      {/* Main Comparison Chart */}
      <div className="h-[400px] w-full bg-[#111111] p-6 rounded-2xl border border-[#2C2C2E]">
         <ResponsiveContainer width="100%" height="100%">
            <LineChart data={mockCompareData}>
              <Tooltip 
                contentStyle={{ backgroundColor: '#000', borderColor: '#2C2C2E', borderRadius: '12px' }}
                itemStyle={{ color: '#fff' }}
              />
              <Line type="monotone" dataKey="strategyA" name="Value" stroke="#00C805" strokeWidth={3} dot={false} />
              <Line type="monotone" dataKey="strategyB" name="Momentum" stroke="#3B82F6" strokeWidth={3} dot={false} />
              <Line type="monotone" dataKey="strategyC" name="Multi-Factor" stroke="#8B5CF6" strokeWidth={3} dot={false} />
            </LineChart>
          </ResponsiveContainer>
      </div>

      {/* Grid Search Rankings Table */}
      <div>
        <h3 className="text-xl font-bold mb-4">Grid Search Optimization (Top 5)</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-[#2C2C2E] text-[#8E8E93]">
                <th className="py-4 px-4 font-medium">Rank</th>
                <th className="py-4 px-4 font-medium">Strategy Type</th>
                <th className="py-4 px-4 font-medium">Parameters</th>
                <th className="py-4 px-4 font-medium text-right">CAGR</th>
                <th className="py-4 px-4 font-medium text-right">MDD</th>
                <th className="py-4 px-4 font-medium text-right">Sharpe</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#2C2C2E]/50">
              {[
                { rank: 1, type: 'Multi-Factor', params: 'Top 20, Val:0.4, Mom:0.4', cagr: '28.4%', mdd: '-11.2%', sharpe: '1.92' },
                { rank: 2, type: 'Multi-Factor', params: 'Top 10, Val:0.5, Mom:0.3', cagr: '26.1%', mdd: '-14.5%', sharpe: '1.85' },
                { rank: 3, type: 'Sector Rotation', params: 'Top 3, 5 Stocks', cagr: '22.8%', mdd: '-16.1%', sharpe: '1.54' },
                { rank: 4, type: 'Value', params: 'Top 20, PER:0.5, PBR:0.5', cagr: '19.4%', mdd: '-18.9%', sharpe: '1.21' },
                { rank: 5, type: 'Dual Momentum', params: 'Max Weight: 1.0', cagr: '15.2%', mdd: '-8.1%', sharpe: '1.65' },
              ].map((row) => (
                <tr key={row.rank} className="hover:bg-[#111111] transition-colors">
                  <td className="py-4 px-4 font-bold">#{row.rank}</td>
                  <td className="py-4 px-4">{row.type}</td>
                  <td className="py-4 px-4 text-sm text-[#8E8E93]">{row.params}</td>
                  <td className="py-4 px-4 text-right font-bold text-[#00C805]">{row.cagr}</td>
                  <td className="py-4 px-4 text-right text-[#FF5000]">{row.mdd}</td>
                  <td className="py-4 px-4 text-right">{row.sharpe}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
