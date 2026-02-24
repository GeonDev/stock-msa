import React, { useState } from 'react';
import { Layers, Loader2, Trophy, Search, RefreshCw, LineChart as ChartIcon } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from 'recharts';
import { useCompareStrategies } from '../hooks/useBacktest';
import { formatPercent } from '../utils/cn';

export default function StrategyCompare() {
  const [resultIdsInput, setResultIdsInput] = useState('1,2,3');
  const [activeIds, setActiveIds] = useState('');
  const formatNum = (val: any, decimals = 1) => (typeof val === 'number' ? val.toFixed(decimals) : '0.0');

  const { data, isLoading, isError, refetch } = useCompareStrategies(activeIds);

  const handleCompare = () => {
    if (resultIdsInput.trim()) {
      setActiveIds(resultIdsInput.replaceAll(' ', ''));
    }
  };

  const chartColors = ['#16a34a', '#2563eb', '#7c3aed', '#d97706', '#db2777', '#0d9488']; // 600 series for light/dark balance

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-20">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div>
          <h2 className="text-3xl md:text-4xl font-black mb-2 tracking-tight">Strategy Compare</h2>
          <p className="text-zinc-500 dark:text-[#8E8E93] font-medium">Evaluate multiple backtest results and grid search rankings.</p>
        </div>
        
        <div className="flex w-full md:w-auto gap-2">
          <div className="relative flex-1 md:w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400 dark:text-[#8E8E93]" size={18} />
            <input 
              type="text" 
              placeholder="e.g. 1,2,3" 
              value={resultIdsInput}
              onChange={(e) => setResultIdsInput(e.target.value)}
              className="w-full bg-white dark:bg-[#111111] border border-zinc-200 dark:border-[#2C2C2E] rounded-xl py-3 pl-10 pr-4 text-black dark:text-white focus:outline-none focus:border-green-500 shadow-sm"
            />
          </div>
          <button 
            onClick={handleCompare}
            className="bg-green-600 dark:bg-[#00C805] hover:bg-green-500 dark:hover:bg-[#00E605] text-white dark:text-black font-bold px-6 py-3 rounded-xl flex items-center gap-2 transition-colors shadow-sm"
          >
            <Layers size={18} />
            <span className="hidden sm:inline">Compare</span>
          </button>
        </div>
      </header>

      {/* Loading & Error States */}
      {isLoading && (
        <div className="h-64 flex flex-col items-center justify-center border-2 border-dashed border-zinc-200 dark:border-[#2C2C2E] rounded-3xl text-zinc-400 dark:text-[#8E8E93] gap-4 bg-white dark:bg-[#111111]">
          <Loader2 className="animate-spin text-green-600 dark:text-[#00C805]" size={32} />
          <p className="font-bold tracking-widest uppercase text-xs">Analyzing Strategies...</p>
        </div>
      )}

      {isError && (
        <div className="h-64 flex flex-col items-center justify-center border-2 border-dashed border-red-500/30 rounded-3xl text-red-600 dark:text-[#FF5000] gap-4 bg-red-500/5">
          <p className="font-bold">Failed to load comparison data.</p>
          <p className="text-sm">Please check if the simulation IDs are correct.</p>
        </div>
      )}

      {/* Main Content */}
      {data && data.results && data.results.length > 0 && (
        <>
          {/* Winner Highlight Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white dark:bg-[#111111] p-6 rounded-2xl border border-green-200 dark:border-[#00C805]/30 relative overflow-hidden shadow-sm">
              <div className="absolute top-0 right-0 p-4 opacity-10">
                <Trophy size={64} color="#16a34a" />
              </div>
              <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase mb-1">Highest Return (CAGR)</p>
              <h3 className="text-2xl font-black text-green-600 dark:text-[#00C805]">ID: {data.bestCagrSimulationId}</h3>
              <p className="text-sm text-zinc-400 dark:text-[#8E8E93] mt-2">Optimal growth configuration</p>
            </div>
            <div className="bg-white dark:bg-[#111111] p-6 rounded-2xl border border-blue-200 dark:border-[#3B82F6]/30 relative overflow-hidden shadow-sm">
              <div className="absolute top-0 right-0 p-4 opacity-10">
                <Trophy size={64} color="#2563eb" />
              </div>
              <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase mb-1">Best Stability (Sharpe)</p>
              <h3 className="text-2xl font-black text-blue-600 dark:text-[#3B82F6]">ID: {data.bestSharpeSimulationId}</h3>
              <p className="text-sm text-zinc-400 dark:text-[#8E8E93] mt-2">Best risk-adjusted return</p>
            </div>
            <div className="bg-white dark:bg-[#111111] p-6 rounded-2xl border border-purple-200 dark:border-[#8B5CF6]/30 relative overflow-hidden shadow-sm">
              <div className="absolute top-0 right-0 p-4 opacity-10">
                <Trophy size={64} color="#7c3aed" />
              </div>
              <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase mb-1">Lowest Risk (MDD)</p>
              <h3 className="text-2xl font-black text-purple-600 dark:text-[#8B5CF6]">ID: {data.lowestMddSimulationId}</h3>
              <p className="text-sm text-zinc-400 dark:text-[#8E8E93] mt-2">Most defensive configuration</p>
            </div>
          </div>

          {/* Metrics Comparison Chart */}
          <div className="bg-white dark:bg-[#111111] p-6 rounded-3xl border border-zinc-200 dark:border-[#2C2C2E] shadow-sm">
            <h3 className="text-xl font-bold mb-6 flex items-center gap-2">
              <ChartIcon size={20} className="text-green-600 dark:text-[#00C805]" /> Performance Overview
            </h3>
            <div className="h-[400px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={data.results}>
                  <CartesianGrid strokeDasharray="3 3" stroke="currentColor" className="text-zinc-200 dark:text-[#2C2C2E]" vertical={false} />
                  <XAxis 
                    dataKey="simulationId" 
                    stroke="currentColor" 
                    className="text-zinc-400 dark:text-[#8E8E93]"
                    fontSize={12} 
                    tickLine={false} 
                    axisLine={false}
                    tickFormatter={(id) => `#${id}`}
                  />
                  <YAxis stroke="currentColor" className="text-zinc-400 dark:text-[#8E8E93]" fontSize={12} tickLine={false} axisLine={false} />
                  <Tooltip 
                    contentStyle={{ 
                      backgroundColor: 'var(--tooltip-bg, #fff)', 
                      border: '1px solid var(--tooltip-border, #e4e4e7)', 
                      borderRadius: '16px' 
                    }}
                    itemStyle={{ fontWeight: 'bold' }}
                  />
                  <Legend />
                  <Line type="monotone" dataKey="cagr" name="CAGR (%)" stroke="#16a34a" strokeWidth={3} dot={{ r: 6 }} activeDot={{ r: 8 }} />
                  <Line type="monotone" dataKey="winRate" name="Win Rate (%)" stroke="#2563eb" strokeWidth={2} dot={{ r: 4 }} />
                  <Line type="monotone" dataKey="sharpeRatio" name="Sharpe Ratio" stroke="#d97706" strokeWidth={2} dot={{ r: 4 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Rankings Table */}
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <h3 className="text-xl font-bold">Optimization Rankings</h3>
              <button onClick={() => refetch()} className="text-zinc-400 dark:text-[#8E8E93] hover:text-black dark:hover:text-white p-2 transition-colors">
                <RefreshCw size={16} />
              </button>
            </div>
            <div className="overflow-x-auto bg-white dark:bg-[#111111] rounded-2xl border border-zinc-200 dark:border-[#2C2C2E] shadow-sm">
              <table className="w-full text-left border-collapse whitespace-nowrap">
                <thead>
                  <tr className="border-b border-zinc-200 dark:border-[#2C2C2E] bg-zinc-50 dark:bg-black/50 text-zinc-500 dark:text-[#8E8E93] text-xs uppercase tracking-wider">
                    <th className="py-4 px-6 font-bold">Sim ID</th>
                    <th className="py-4 px-6 font-bold">Optimized</th>
                    <th className="py-4 px-6 font-bold">Slippage</th>
                    <th className="py-4 px-6 font-bold text-right">Trades</th>
                    <th className="py-4 px-6 font-bold text-right">Win Rate</th>
                    <th className="py-4 px-6 font-bold text-right">MDD</th>
                    <th className="py-4 px-6 font-bold text-right">Sharpe</th>
                    <th className="py-4 px-6 font-bold text-right">CAGR</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 dark:divide-[#2C2C2E]/50">
                  {data.results
                    .sort((a: any, b: any) => b.cagr - a.cagr)
                    .map((row: any, idx: number) => {
                      const isBestCagr = row.simulationId === data.bestCagrSimulationId;
                      return (
                        <tr key={row.simulationId} className={`transition-colors ${isBestCagr ? 'bg-green-50 dark:bg-[#00C805]/5' : 'hover:bg-zinc-50 dark:hover:bg-black/40'}`}>
                          <td className="py-4 px-6 font-bold">
                            <div className="flex items-center gap-2">
                              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: chartColors[idx % chartColors.length] }}></div>
                              #{row.simulationId}
                            </div>
                          </td>
                          <td className="py-4 px-6 text-sm">
                            <span className={`px-2 py-1 rounded-md ${row.isOptimized ? 'bg-green-100 dark:bg-[#00C805]/20 text-green-700 dark:text-[#00C805]' : 'bg-zinc-100 dark:bg-[#2C2C2E] text-zinc-500 dark:text-[#8E8E93]'}`}>
                              {row.isOptimized ? 'Grid Search' : 'Manual'}
                            </span>
                          </td>
                          <td className="py-4 px-6 text-sm text-zinc-500 dark:text-[#8E8E93]">{row.slippageType}</td>
                          <td className="py-4 px-6 text-right font-medium">{row.totalTrades}</td>
                          <td className="py-4 px-6 text-right font-medium">{formatPercent(row.winRate)}</td>
                          <td className="py-4 px-6 text-right font-medium text-red-600 dark:text-[#FF5000]">{formatPercent(row.mdd)}</td>
                          <td className="py-4 px-6 text-right font-medium">{formatNum(row.sharpeRatio, 2)}</td>
                          <td className={`py-4 px-6 text-right font-black ${isBestCagr ? 'text-green-600 dark:text-[#00C805] text-lg' : 'text-black dark:text-white'}`}>
                            {formatPercent(row.cagr)}
                          </td>
                        </tr>
                      );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* Initial Empty State */}
      {!data && !isLoading && !isError && (
        <div className="h-64 flex flex-col items-center justify-center border-2 border-dashed border-zinc-200 dark:border-[#2C2C2E] rounded-3xl text-zinc-400 dark:text-[#8E8E93] gap-4 bg-white dark:bg-[#111111]">
          <Layers size={48} className="opacity-20" />
          <p className="font-bold text-center">Enter comma-separated Simulation IDs<br/>to compare performances.</p>
        </div>
      )}
    </div>
  );
}
