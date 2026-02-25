import React, { useState, useEffect } from 'react';
import { Play, Settings2, Loader2, Info } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { useBacktest, useBacktestResult, useSnapshots } from '../hooks/useBacktest';
import { formatCurrency, formatPercent } from '../utils/cn';
import { BacktestRequest } from '../types/api';

export default function BacktestSimulator() {
  const [simulationId, setSimulationId] = useState<number | null>(null);
  const [isWaiting, setIsWaiting] = useState(false);
  const [activeTab, setActiveTab] = useState<'general' | 'costs' | 'strategy'>('general');
  const formatNum = (val: any) => (typeof val === 'number' ? val.toFixed(1) : '0.0');

  const backtestMutation = useBacktest();
  const { data: result } = useBacktestResult(simulationId);
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
    maxVolumeRatio: 0.05,
    valueStrategyConfig: {
      topN: 20,
      perWeight: 0.3,
      pbrWeight: 0.3,
      roeWeight: 0.4
    },
    multiFactorConfig: {
      topN: 20,
      factors: [
        { factor: 'PER', weight: 0.2, inverse: true },
        { factor: 'PBR', weight: 0.2, inverse: true },
        { factor: 'GP_A', weight: 0.3, inverse: false },
        { factor: 'MOMENTUM_6M', weight: 0.3, inverse: false }
      ]
    },
    sectorRotationConfig: {
      topN: 3,
      lookbackPeriod: 12
    }
  });

  const handleRun = () => {
    setIsWaiting(true);
    backtestMutation.mutate(formData, {
      onSuccess: (data: any) => {
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

  const chartData = snapshots?.map((s: any) => ({
    date: s.snapshotDate,
    value: s.totalValue,
  })) || [];

  const isPositive = result ? result.totalReturn >= 0 : true;
  const mainColor = isPositive ? '#16a34a' : '#dc2626'; // green-600, red-600 for light/dark balance

  return (
    <div className="flex flex-col lg:flex-row gap-8 min-h-screen pb-20">
      
      {/* Parameters Panel */}
      <div className="w-full lg:w-96 bg-white dark:bg-[#111111] p-6 rounded-3xl border border-zinc-200 dark:border-[#2C2C2E] flex flex-col gap-6 self-start sticky top-8 shadow-sm">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Settings2 className="text-zinc-400 dark:text-[#8E8E93]" size={20} />
            <h2 className="text-xl font-bold">Parameters</h2>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex bg-zinc-100 dark:bg-black p-1 rounded-xl">
          {(['general', 'costs', 'strategy'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex-1 py-2 text-[10px] font-black uppercase tracking-tighter rounded-lg transition-all ${
                activeTab === tab 
                  ? 'bg-white dark:bg-[#1c1c1e] text-green-600 dark:text-[#00C805] shadow-sm' 
                  : 'text-zinc-400 hover:text-zinc-600'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="space-y-5 min-h-[400px]">
          {activeTab === 'general' && (
            <div className="space-y-5 animate-in fade-in duration-300">
              <div className="space-y-2">
                <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Strategy Type</label>
                <select 
                  className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500 appearance-none cursor-pointer"
                  value={formData.strategyType}
                  onChange={(e) => setFormData({...formData, strategyType: e.target.value})}
                >
                  <option value="VALUE">Value (PBR/ROE/PER)</option>
                  <option value="MULTI_FACTOR">Multi-Factor</option>
                  <option value="SECTOR_ROTATION">Sector Rotation</option>
                  <option value="DUAL_MOMENTUM">Dual Momentum</option>
                  <option value="RISK_PARITY">Risk Parity</option>
                </select>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Initial Capital (₩)</label>
                <input 
                  type="number" 
                  className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                  value={formData.initialCapital}
                  onChange={(e) => setFormData({...formData, initialCapital: Number(e.target.value)})}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Start</label>
                  <input 
                    type="date" 
                    className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                    value={formData.startDate}
                    onChange={(e) => setFormData({...formData, startDate: e.target.value})}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">End</label>
                  <input 
                    type="date" 
                    className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                    value={formData.endDate}
                    onChange={(e) => setFormData({...formData, endDate: e.target.value})}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Rebalancing</label>
                <select 
                  className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500 appearance-none cursor-pointer"
                  value={formData.rebalancingPeriod}
                  onChange={(e) => setFormData({...formData, rebalancingPeriod: e.target.value})}
                >
                  <option value="DAILY">Daily</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option>
                  <option value="QUARTERLY">Quarterly</option>
                  <option value="YEARLY">Yearly</option>
                </select>
              </div>
            </div>
          )}

          {activeTab === 'costs' && (
            <div className="space-y-5 animate-in fade-in duration-300">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Fee Rate (%)</label>
                  <input 
                    type="number" step="0.0001"
                    className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                    value={formData.tradingFeeRate * 100}
                    onChange={(e) => setFormData({...formData, tradingFeeRate: Number(e.target.value) / 100})}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Tax Rate (%)</label>
                  <input 
                    type="number" step="0.0001"
                    className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                    value={formData.taxRate * 100}
                    onChange={(e) => setFormData({...formData, taxRate: Number(e.target.value) / 100})}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Slippage Model</label>
                <select 
                  className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                  value={formData.slippageType}
                  onChange={(e) => setFormData({...formData, slippageType: e.target.value})}
                >
                  <option value="NONE">None</option>
                  <option value="FIXED">Fixed Rate</option>
                  <option value="VOLUME">Volume-Based</option>
                </select>
              </div>

              {formData.slippageType === 'FIXED' && (
                <div className="space-y-2">
                  <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Slippage Rate (%)</label>
                  <input 
                    type="number" step="0.01"
                    className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                    value={(formData.fixedSlippageRate || 0) * 100}
                    onChange={(e) => setFormData({...formData, fixedSlippageRate: Number(e.target.value) / 100})}
                  />
                </div>
              )}

              <div className="space-y-2">
                <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Max Stock Weight (%)</label>
                <input 
                  type="number" step="1"
                  className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                  value={(formData.maxWeightPerStock || 0) * 100}
                  onChange={(e) => setFormData({...formData, maxWeightPerStock: Number(e.target.value) / 100})}
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Max Volume Ratio (%)</label>
                <input 
                  type="number" step="1"
                  className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                  value={(formData.maxVolumeRatio || 0) * 100}
                  onChange={(e) => setFormData({...formData, maxVolumeRatio: Number(e.target.value) / 100})}
                />
              </div>
            </div>
          )}

          {activeTab === 'strategy' && (
            <div className="space-y-5 animate-in fade-in duration-300">
              {formData.strategyType === 'VALUE' && (
                <>
                  <div className="space-y-2">
                    <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Top N Stocks</label>
                    <input 
                      type="number"
                      className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                      value={formData.valueStrategyConfig?.topN}
                      onChange={(e) => setFormData({
                        ...formData, 
                        valueStrategyConfig: { ...formData.valueStrategyConfig, topN: Number(e.target.value) }
                      })}
                    />
                  </div>
                  <div className="grid grid-cols-3 gap-2">
                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-zinc-400 uppercase">PER W.</label>
                      <input type="number" step="0.1" className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-xl p-2 text-xs"
                        value={formData.valueStrategyConfig?.perWeight}
                        onChange={(e) => setFormData({...formData, valueStrategyConfig: {...formData.valueStrategyConfig, perWeight: Number(e.target.value)}})}
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-zinc-400 uppercase">PBR W.</label>
                      <input type="number" step="0.1" className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-xl p-2 text-xs"
                        value={formData.valueStrategyConfig?.pbrWeight}
                        onChange={(e) => setFormData({...formData, valueStrategyConfig: {...formData.valueStrategyConfig, pbrWeight: Number(e.target.value)}})}
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-zinc-400 uppercase">ROE W.</label>
                      <input type="number" step="0.1" className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-xl p-2 text-xs"
                        value={formData.valueStrategyConfig?.roeWeight}
                        onChange={(e) => setFormData({...formData, valueStrategyConfig: {...formData.valueStrategyConfig, roeWeight: Number(e.target.value)}})}
                      />
                    </div>
                  </div>
                </>
              )}

              {formData.strategyType === 'SECTOR_ROTATION' && (
                <>
                  <div className="space-y-2">
                    <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Top N Sectors</label>
                    <input 
                      type="number"
                      className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                      value={formData.sectorRotationConfig?.topN}
                      onChange={(e) => setFormData({
                        ...formData, 
                        sectorRotationConfig: { ...formData.sectorRotationConfig, topN: Number(e.target.value) }
                      })}
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Lookback (Months)</label>
                    <input 
                      type="number"
                      className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl p-4 text-black dark:text-white focus:outline-none focus:border-green-500"
                      value={formData.sectorRotationConfig?.lookbackPeriod}
                      onChange={(e) => setFormData({
                        ...formData, 
                        sectorRotationConfig: { ...formData.sectorRotationConfig, lookbackPeriod: Number(e.target.value) }
                      })}
                    />
                  </div>
                </>
              )}

              {/* ... Other strategy configs can be added here ... */}
              {formData.strategyType !== 'VALUE' && formData.strategyType !== 'SECTOR_ROTATION' && (
                <div className="p-8 text-center border-2 border-dashed border-zinc-100 dark:border-zinc-800 rounded-2xl text-zinc-400 text-xs italic">
                  Additional parameters for this strategy will be available soon.
                </div>
              )}
            </div>
          )}
        </div>

        <button 
          onClick={handleRun}
          disabled={isWaiting || backtestMutation.isPending}
          className="mt-4 w-full bg-green-600 dark:bg-[#00C805] hover:bg-green-500 dark:hover:bg-[#00E605] text-white dark:text-black font-black py-5 rounded-2xl flex justify-center items-center gap-3 transition-all active:scale-95 disabled:opacity-50 disabled:active:scale-100 shadow-lg shadow-green-500/20"
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
            <div className="h-48 flex items-center justify-center border-2 border-dashed border-zinc-200 dark:border-[#2C2C2E] rounded-3xl text-zinc-400 dark:text-[#8E8E93] gap-2">
              <Info size={20} />
              <span>Configure and run a simulation to see results.</span>
            </div>
          ) : (
            <div className="flex flex-col gap-1">
              <p className="text-zinc-500 dark:text-[#8E8E93] font-bold text-sm uppercase tracking-widest">Portfolio Value</p>
              <div className="flex items-baseline gap-4 flex-wrap">
                <h1 className="text-6xl md:text-7xl font-black tracking-tighter">
                  {isWaiting ? 'Evaluating...' : formatCurrency(result?.finalValue || 0)}
                </h1>
                {result && (
                  <span className={`text-2xl font-black ${isPositive ? 'text-green-600 dark:text-[#00C805]' : 'text-red-600 dark:text-[#FF5000]'}`}>
                    {formatPercent(result.totalReturn)}
                  </span>
                )}
              </div>
              
              {result && (
                <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mt-8 border-t border-zinc-200 dark:border-[#2C2C2E] pt-8">
                  <div>
                    <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase mb-1">CAGR</p>
                    <p className="text-2xl font-black text-green-600 dark:text-[#00C805]">{formatNum(result.cagr)}%</p>
                  </div>
                  <div>
                    <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase mb-1">Max Drawdown</p>
                    <p className="text-2xl font-black text-red-600 dark:text-[#FF5000]">{formatNum(result.mdd)}%</p>
                  </div>
                  <div>
                    <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase mb-1">Sharpe Ratio</p>
                    <p className="text-2xl font-black">{formatNum(result.sharpeRatio)}</p>
                  </div>
                  <div>
                    <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase mb-1">Win Rate</p>
                    <p className="text-2xl font-black">{formatNum(result.winRate)}%</p>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Interactive Chart */}
        <div className="h-[500px] w-full bg-zinc-50 dark:bg-black rounded-3xl relative overflow-hidden border border-zinc-100 dark:border-transparent">
          {isWaiting && (
            <div className="absolute inset-0 z-10 bg-white/40 dark:bg-black/40 backdrop-blur-sm flex items-center justify-center">
               <Loader2 className="animate-spin text-green-600 dark:text-[#00C805]" size={48} />
            </div>
          )}
          
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData} margin={{ top: 20, right: 30, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor={mainColor} stopOpacity={0.3}/>
                  <stop offset="95%" stopColor={mainColor} stopOpacity={0}/>
                </linearGradient>
              </defs>
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: 'var(--tooltip-bg, #fff)', 
                  border: '1px solid var(--tooltip-border, #e4e4e7)', 
                  borderRadius: '16px', 
                  padding: '12px' 
                }}
                itemStyle={{ color: 'var(--tooltip-text, #000)', fontWeight: 'bold' }}
                labelStyle={{ color: '#8E8E93', marginBottom: '4px' }}
                formatter={(val: number | undefined) => [val !== undefined ? formatCurrency(val) : '', 'Value']}
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
           <div className="bg-white dark:bg-[#111111] rounded-3xl border border-zinc-200 dark:border-[#2C2C2E] overflow-hidden shadow-sm">
              <div className="p-6 border-b border-zinc-200 dark:border-[#2C2C2E] flex justify-between items-center">
                <h3 className="font-black text-xl">Simulation Metadata</h3>
                <span className="bg-zinc-100 dark:bg-black px-4 py-1 rounded-full text-xs font-bold text-zinc-500 dark:text-[#8E8E93] border border-zinc-200 dark:border-[#2C2C2E]">
                   ID: {result.simulationId}
                </span>
              </div>
              <div className="p-6 grid grid-cols-2 md:grid-cols-3 gap-6">
                 <div>
                    <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase">Total Trades</p>
                    <p className="text-xl font-bold">{result.totalTrades}</p>
                 </div>
                 <div>
                    <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase">Slippage Model</p>
                    <p className="text-xl font-bold">{result.slippageType}</p>
                 </div>
                 <div>
                    <p className="text-zinc-500 dark:text-[#8E8E93] text-xs font-bold uppercase">Optimized</p>
                    <p className="text-xl font-bold text-green-600 dark:text-[#00C805]">{result.isOptimized ? 'Yes' : 'No'}</p>
                 </div>
              </div>
           </div>
        )}

      </div>
    </div>
  );
}
