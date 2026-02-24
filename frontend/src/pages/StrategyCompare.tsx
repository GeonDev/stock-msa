import React, { useState } from 'react';
import { Layers, Loader2, Trophy, Search, RefreshCw } from 'lucide-react';
import { LineChart, Line, XAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from 'recharts';
import { useCompareStrategies } from '../hooks/useBacktest';
import { formatPercent } from '../utils/cn';

export default function StrategyCompare() {
  const [resultIdsInput, setResultIdsInput] = useState('1,2,3');
  const [activeIds, setActiveIds] = useState('');

  const { data, isLoading, isError, refetch } = useCompareStrategies(activeIds);

  const handleCompare = () => {
    if (resultIdsInput.trim()) {
      setActiveIds(resultIdsInput.replaceAll(' ', ''));
    }
  };

  const chartColors = ['#00C805', '#3B82F6', '#8B5CF6', '#F59E0B', '#EC4899', '#14B8A6'];

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-20">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div>
          <h2 className="text-3xl md:text-4xl font-black mb-2 tracking-tight">Strategy Compare</h2>
          <p className="text-[#8E8E93] font-medium">Evaluate multiple backtest results and grid search rankings.</p>
        </div>
        
        <div className="flex w-full md:w-auto gap-2">
          <div className="relative flex-1 md:w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-[#8E8E93]" size={18} />
            <input 
              type="text" 
              placeholder="e.g. 10,11,12" 
              value={resultIdsInput}
              onChange={(e) => setResultIdsInput(e.target.value)}
              className="w-full bg-[#111111] border border-[#2C2C2E] rounded-xl py-3 pl-10 pr-4 text-white focus:outline-none focus:border-[#00C805]"
            />
          </div>
          <button 
            onClick={handleCompare}
            className="bg-[#00C805] hover:bg-[#00E605] text-black font-bold px-6 py-3 rounded-xl flex items-center gap-2 transition-colors"
          >
            <Layers size={18} />
            <span className="hidden sm:inline">Compare</span>
          </button>
        </div>
      </header>

      {/* Loading & Error States */}
      {isLoading && (
        <div className="h-64 flex flex-col items-center justify-center border-2 border-dashed border-[#2C2C2E] rounded-3xl text-[#8E8E93] gap-4">
          <Loader2 className="animate-spin text-[#00C805]" size={32} />
          <p className="font-bold tracking-widest uppercase text-xs">Analyzing Strategies...</p>
        </div>
      )}

      {isError && (
        <div className="h-64 flex flex-col items-center justify-center border-2 border-dashed border-red-500/30 rounded-3xl text-[#FF5000] gap-4 bg-red-500/5">
          <p className="font-bold">Failed to load comparison data.</p>
          <p className="text-sm">Please check if the simulation IDs are correct.</p>
        </div>
      )}

      {/* Main Content */}
      {data && data.results && data.results.length > 0 && (
        <>
          {/* Winner Highlight Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-[#111111] p-6 rounded-2xl border border-[#00C805]/30 relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10">
                <Trophy size={64} color="#00C805" />
              </div>
              <p className="text-[#8E8E93] text-xs font-bold uppercase mb-1">Highest Return (CAGR)</p>
              <h3 className="text-2xl font-black text-[#00C805]">ID: {data.bestCagrSimulationId}</h3>
              <p className="text-sm text-[#8E8E93] mt-2">Optimal growth configuration</p>
            </div>
            <div className="bg-[#111111] p-6 rounded-2xl border border-[#3B82F6]/30 relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10">
                <Trophy size={64} color="#3B82F6" />
              </div>
              <p className="text-[#8E8E93] text-xs font-bold uppercase mb-1">Best Stability (Sharpe)</p>
              <h3 className="text-2xl font-black text-[#3B82F6]">ID: {data.bestSharpeSimulationId}</h3>
              <p className="text-sm text-[#8E8E93] mt-2">Best risk-adjusted return</p>
            </div>
            <div className="bg-[#111111] p-6 rounded-2xl border border-[#8B5CF6]/30 relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10">
                <Trophy size={64} color="#8B5CF6" />
              </div>
              <p className="text-[#8E8E93] text-xs font-bold uppercase mb-1">Lowest Risk (MDD)</p>
              <h3 className="text-2xl font-black text-[#8B5CF6]">ID: {data.lowestMddSimulationId}</h3>
              <p className="text-sm text-[#8E8E93] mt-2">Most defensive configuration</p>
            </div>
          </div>

          {/* Grid Search Rankings Table */}
          <div>
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-xl font-bold">Optimization Rankings</h3>
              <button onClick={() => refetch()} className="text-[#8E8E93] hover:text-white p-2">
                <RefreshCw size={16} />
              </button>
            </div>
            <div className="overflow-x-auto bg-[#111111] rounded-2xl border border-[#2C2C2E]">
              <table className="w-full text-left border-collapse whitespace-nowrap">
                <thead>
                  <tr className="border-b border-[#2C2C2E] bg-black/50 text-[#8E8E93] text-xs uppercase tracking-wider">
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
                <tbody className="divide-y divide-[#2C2C2E]/50">
                  {data.results
                    .sort((a: any, b: any) => b.cagr - a.cagr) // 기본 CAGR 내림차순 정렬
                    .map((row: any, idx: number) => {
                      const isBestCagr = row.simulationId === data.bestCagrSimulationId;
                      return (
                        <tr key={row.simulationId} className={`transition-colors ${isBestCagr ? 'bg-[#00C805]/5' : 'hover:bg-black/40'}`}>
                          <td className="py-4 px-6 font-bold">
                            <div className="flex items-center gap-2">
                              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: chartColors[idx % chartColors.length] }}></div>
                              #{row.simulationId}
                            </div>
                          </td>
                          <td className="py-4 px-6 text-sm">
                            <span className={`px-2 py-1 rounded-md ${row.isOptimized ? 'bg-[#00C805]/20 text-[#00C805]' : 'bg-[#2C2C2E] text-[#8E8E93]'}`}>
                              {row.isOptimized ? 'Grid Search' : 'Manual'}
                            </span>
                          </td>
                          <td className="py-4 px-6 text-sm text-[#8E8E93]">{row.slippageType}</td>
                          <td className="py-4 px-6 text-right font-medium">{row.totalTrades}</td>
                          <td className="py-4 px-6 text-right font-medium">{formatPercent(row.winRate)}</td>
                          <td className="py-4 px-6 text-right font-medium text-[#FF5000]">{formatPercent(row.mdd)}</td>
                          <td className="py-4 px-6 text-right font-medium">{row.sharpeRatio?.toFixed(2) || '0.00'}</td>
                          <td className={`py-4 px-6 text-right font-black ${isBestCagr ? 'text-[#00C805] text-lg' : 'text-white'}`}>
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
        <div className="h-64 flex flex-col items-center justify-center border-2 border-dashed border-[#2C2C2E] rounded-3xl text-[#8E8E93] gap-4">
          <Layers size={48} className="opacity-20" />
          <p className="font-bold text-center">Enter comma-separated Simulation IDs<br/>to compare performances.</p>
        </div>
      )}
    </div>
  );
}
