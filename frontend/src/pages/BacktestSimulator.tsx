import React, { useState, useEffect } from 'react';
import { Play, Settings2, Loader2, Info } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { useBacktest, useBacktestResult, useSnapshots } from '../hooks/useBacktest';
import { formatCurrency, formatPercent } from '../utils/cn';
import { BacktestRequest } from '../types/api';

export default function BacktestSimulator() {
  const [simulationId, setSimulationId] = useState<number | null>(null);
  const [isWaiting, setIsWaiting] = useState(false);

  const backtestMutation = useBacktest();
  const { data: result, isLoading: isLoadingResult } = useBacktestResult(simulationId);
  const { data: snapshots } = useSnapshots(simulationId);

  const [formData, setFormData] = useState<BacktestRequest>({
    strategyType: 'VALUE',
    startDate: '2023-01-01',
    endDate: '2023-12-31',
    initialCapital: 10000000,
    rebalancingPeriod: 'MONTHLY',
    tradingFeeRate: 0.0015,
    taxRate: 0.002,
    slippageType: 'FIXED',
    fixedSlippageRate: 0.002,
    maxWeightPerStock: 0.2,
    valueStrategyConfig: {
      topN: 20,
      perWeight: 0.3,
      pbrWeight: 0.3,
      roeWeight: 0.4
    }
  });

  const handleRun = () => {
    setIsWaiting(true);
    backtestMutation.mutate(formData, {
      onSuccess: (data) => {
        setSimulationId(data.simulationId);
      },
      onError: () => {
        setIsWaiting(false);
      }
    });
  };

  useEffect(() => {
    if (result) {
      setIsWaiting(false);
    }
  }, [result]);

  const chartData = snapshots?.map(s => ({
    date: s.snapshotDate,
    value: s.totalValue,
  })) || [];

  const isPositive = result ? result.totalReturn >= 0 : true;
  const mainColor = isPositive ? '#00C805' : '#FF5000';

  return (
    <div className="flex flex-col lg:flex-row gap-8 min-h-screen pb-20">
      
      {/* Parameters Panel */}
      <div className="w-full lg:w-96 bg-[#111111] p-6 rounded-3xl border border-[#2C2C2E] flex flex-col gap-6 self-start sticky top-8">
        <div className="flex items-center gap-2">
          <Settings2 className="text-[#8E8E93]" size={20} />
          <h2 className="text-xl font-bold">Parameters</h2>
        </div>

        <div className="space-y-5">
          <div className="space-y-2">
            <label className="text-xs font-bold text-[#8E8E93] uppercase tracking-wider">Strategy</label>
            <select 
              className="w-full bg-black border border-[#2C2C2E] rounded-2xl p-4 text-white focus:outline-none focus:border-[#00C805] appearance-none cursor-pointer"
              value={formData.strategyType}
              onChange={(e) => setFormData({...formData, strategyType: e.target.value})}
            >
              <option value="VALUE">Value (PER/PBR)</option>
              <option value="MOMENTUM">Momentum</option>
              <option value="MULTI_FACTOR">Multi-Factor (Z-Score)</option>
              <option value="SECTOR_ROTATION">Sector Rotation</option>
              <option value="DUAL_MOMENTUM">Dual Momentum</option>
              <option value="RISK_PARITY">Risk Parity</option>
            </select>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-bold text-[#8E8E93] uppercase tracking-wider">Capital (₩)</label>
            <input 
              type="number" 
              className="w-full bg-black border border-[#2C2C2E] rounded-2xl p-4 text-white focus:outline-none focus:border-[#00C805]"
              value={formData.initialCapital}
              onChange={(e) => setFormData({...formData, initialCapital: Number(e.target.value)})}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-xs font-bold text-[#8E8E93] uppercase tracking-wider">Start</label>
              <input 
                type="date" 
                className="w-full bg-black border border-[#2C2C2E] rounded-2xl p-4 text-white focus:outline-none focus:border-[#00C805]"
                value={formData.startDate}
                onChange={(e) => setFormData({...formData, startDate: e.target.value})}
              />
            </div>
            <div className="space-y-2">
              <label className="text-xs font-bold text-[#8E8E93] uppercase tracking-wider">End</label>
              <input 
                type="date" 
                className="w-full bg-black border border-[#2C2C2E] rounded-2xl p-4 text-white focus:outline-none focus:border-[#00C805]"
                value={formData.endDate}
                onChange={(e) => setFormData({...formData, endDate: e.target.value})}
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-bold text-[#8E8E93] uppercase tracking-wider">Slippage</label>
            <select 
              className="w-full bg-black border border-[#2C2C2E] rounded-2xl p-4 text-white focus:outline-none focus:border-[#00C805]"
              value={formData.slippageType}
              onChange={(e) => setFormData({...formData, slippageType: e.target.value})}
            >
              <option value="FIXED">Fixed (0.2%)</option>
              <option value="VOLUME">Volume-Based</option>
              <option value="NONE">No Slippage</option>
            </select>
          </div>
        </div>

        <button 
          onClick={handleRun}
          disabled={isWaiting || backtestMutation.isPending}
          className="mt-4 w-full bg-[#00C805] hover:bg-[#00E605] text-black font-black py-5 rounded-2xl flex justify-center items-center gap-3 transition-all active:scale-95 disabled:opacity-50 disabled:active:scale-100"
        >
          {isWaiting ? <Loader2 className="animate-spin" size={24} /> : <Play size={20} fill="currentColor" />}
          {isWaiting ? 'Processing...' : 'Run Simulation'}
        </button>
      </div>

      {/* Results Display */}
      <div className="flex-1 flex flex-col gap-10">
        
        {/* KPI Hero */}
        <div className="animate-in fade-in slide-in-from-top-4 duration-700">
          {!result && !isWaiting ? (
            <div className="h-48 flex items-center justify-center border-2 border-dashed border-[#2C2C2E] rounded-3xl text-[#8E8E93] gap-2">
              <Info size={20} />
              <span>Configure and run a simulation to see results.</span>
            </div>
          ) : (
            <div className="flex flex-col gap-1">
              <p className="text-[#8E8E93] font-bold text-sm uppercase tracking-widest">Portfolio Value</p>
              <div className="flex items-baseline gap-4 flex-wrap">
                <h1 className="text-6xl md:text-7xl font-black tracking-tighter">
                  {isWaiting ? 'Evaluating...' : formatCurrency(result?.finalValue || 0)}
                </h1>
                {result && (
                  <span className={`text-2xl font-black ${isPositive ? 'text-[#00C805]' : 'text-[#FF5000]'}`}>
                    {formatPercent(result.totalReturn)}
                  </span>
                )}
              </div>
              
              {result && (
                <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mt-8 border-t border-[#2C2C2E] pt-8">
                  <div>
                    <p className="text-[#8E8E93] text-xs font-bold uppercase mb-1">CAGR</p>
                    <p className="text-2xl font-black text-[#00C805]">{result.cagr}%</p>
                  </div>
                  <div>
                    <p className="text-[#8E8E93] text-xs font-bold uppercase mb-1">Max Drawdown</p>
                    <p className="text-2xl font-black text-[#FF5000]">{result.mdd}%</p>
                  </div>
                  <div>
                    <p className="text-[#8E8E93] text-xs font-bold uppercase mb-1">Sharpe Ratio</p>
                    <p className="text-2xl font-black">{result.sharpeRatio}</p>
                  </div>
                  <div>
                    <p className="text-[#8E8E93] text-xs font-bold uppercase mb-1">Win Rate</p>
                    <p className="text-2xl font-black">{result.winRate}%</p>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Interactive Chart */}
        <div className="h-[500px] w-full bg-black rounded-3xl relative overflow-hidden">
          {isWaiting && (
            <div className="absolute inset-0 z-10 bg-black/40 backdrop-blur-sm flex items-center justify-center">
               <Loader2 className="animate-spin text-[#00C805]" size={48} />
            </div>
          )}
          
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData}>
              <defs>
                <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor={mainColor} stopOpacity={0.3}/>
                  <stop offset="95%" stopColor={mainColor} stopOpacity={0}/>
                </linearGradient>
              </defs>
              <Tooltip 
                contentStyle={{ backgroundColor: '#111', border: '1px solid #2C2C2E', borderRadius: '16px', padding: '12px' }}
                itemStyle={{ color: '#fff', fontWeight: 'bold' }}
                labelStyle={{ color: '#8E8E93', marginBottom: '4px' }}
                formatter={(val: number) => [formatCurrency(val), 'Value']}
              />
              <Area 
                type="monotone" 
                dataKey="value" 
                stroke={mainColor} 
                strokeWidth={4} 
                fillOpacity={1} 
                fill="url(#colorValue)" 
                animationDuration={1500}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Trades / Portfolio Table */}
        {result && (
           <div className="bg-[#111111] rounded-3xl border border-[#2C2C2E] overflow-hidden">
              <div className="p-6 border-b border-[#2C2C2E] flex justify-between items-center">
                <h3 className="font-black text-xl">Simulation Metadata</h3>
                <span className="bg-black px-4 py-1 rounded-full text-xs font-bold text-[#8E8E93] border border-[#2C2C2E]">
                   ID: {result.simulationId}
                </span>
              </div>
              <div className="p-6 grid grid-cols-2 md:grid-cols-3 gap-6">
                 <div>
                    <p className="text-[#8E8E93] text-xs font-bold uppercase">Total Trades</p>
                    <p className="text-xl font-bold">{result.totalTrades}</p>
                 </div>
                 <div>
                    <p className="text-[#8E8E93] text-xs font-bold uppercase">Slippage Model</p>
                    <p className="text-xl font-bold">{result.slippageType}</p>
                 </div>
                 <div>
                    <p className="text-[#8E8E93] text-xs font-bold uppercase">Optimized</p>
                    <p className="text-xl font-bold text-[#00C805]">{result.isOptimized ? 'Yes' : 'No'}</p>
                 </div>
              </div>
           </div>
        )}

      </div>
    </div>
  );
}
