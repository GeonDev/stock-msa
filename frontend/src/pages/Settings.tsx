import React, { useState } from 'react';
import { Sun, Moon, Monitor, Server, Play, ShieldCheck, Database, RefreshCw, Cpu, Bell, Eye, Trash2 } from 'lucide-react';
import { useSettingsStore } from '../hooks/useSettingsStore';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { systemService, corpService, financeService, priceService, aiService } from '../services/api';
import { UserAlert } from '../types/api';

const SettingSection = ({ title, icon: Icon, children }: any) => (
  <div className="bg-white dark:bg-[#111111] p-6 rounded-3xl border border-zinc-200 dark:border-[#2C2C2E] shadow-sm">
    <div className="flex items-center gap-3 mb-6">
      <div className="p-2 bg-zinc-100 dark:bg-black rounded-xl">
        <Icon className="text-zinc-500 dark:text-[#8E8E93]" size={20} />
      </div>
      <h3 className="text-lg font-bold">{title}</h3>
    </div>
    {children}
  </div>
);

const BatchButton = ({ label, onClick, isLoading }: any) => (
  <button 
    onClick={onClick}
    disabled={isLoading}
    className="flex items-center justify-between w-full p-4 bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-2xl hover:border-green-500 transition-all disabled:opacity-50 group"
  >
    <span className="font-medium text-sm">{label}</span>
    {isLoading ? <RefreshCw size={16} className="animate-spin text-green-500" /> : <Play size={16} className="text-zinc-400 group-hover:text-green-500" />}
  </button>
);

export default function Settings() {
  const queryClient = useQueryClient();
  const { theme, setTheme, defaults, setDefaults } = useSettingsStore();
  const [chatId, setChatId] = useState('DEFAULT_USER'); // Default for demo

  const getYesterdayDate = () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const year = yesterday.getFullYear();
    const month = String(yesterday.getMonth() + 1).padStart(2, '0');
    const day = String(yesterday.getDate()).padStart(2, '0');
    return {
      fullDate: `${year}${month}${day}`,
      year: `${year}`
    };
  };
  
  // Batch Parameters States
  const [priceStartDate, setPriceStartDate] = useState(getYesterdayDate().fullDate);
  const [priceEndDate, setPriceEndDate] = useState(getYesterdayDate().fullDate);
  const [financeStartYear, setFinanceStartYear] = useState(getYesterdayDate().year);
  const [financeEndYear, setFinanceEndYear] = useState(getYesterdayDate().year);
  const [financeYear, setFinanceYear] = useState(getYesterdayDate().year);
  const [financeDate, setFinanceDate] = useState(getYesterdayDate().fullDate);

  // Service Health Queries
  const { data: gatewayStatus } = useQuery({ queryKey: ['health', 'gateway'], queryFn: () => systemService.getServiceInfo() });

  // AI Service Queries
  const { data: watchlist } = useQuery({ 
    queryKey: ['watchlist', chatId], 
    queryFn: () => aiService.getWatchlist(chatId) 
  });
  const { data: activeAlerts } = useQuery({ 
    queryKey: ['alerts', 'active'], 
    queryFn: () => aiService.getActiveAlerts() 
  });

  // AI Mutations
  const removeWatchlistMutation = useMutation({
    mutationFn: (ticker: string) => aiService.removeFromWatchlist(chatId, ticker),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['watchlist', chatId] })
  });

  // Batch Mutations
  const corpBatch = useMutation({ mutationFn: (date: string) => corpService.runCorpInfoBatch(date) });
  const priceRecoveryMutation = useMutation({ 
    mutationFn: () => priceService.runPriceRecovery(priceStartDate, priceEndDate) 
  });
  const financeRecoveryMutation = useMutation({ 
    mutationFn: () => financeService.runFinanceRecovery(financeStartYear, financeEndYear) 
  });
  const financeBatch = useMutation({ 
    mutationFn: (code: string) => financeService.runFinanceBatch(financeYear, code) 
  });

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700 pb-20">
      <header>
        <h2 className="text-3xl md:text-4xl font-black mb-2">Settings</h2>
        <p className="text-zinc-500 dark:text-[#8E8E93]">System configuration and infrastructure control.</p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Watchlist Section */}
        <SettingSection title="My Watchlist" icon={Eye}>
          <div className="space-y-4">
            <div className="flex gap-2 mb-4">
              <input 
                type="text" 
                value={chatId}
                onChange={(e) => setChatId(e.target.value)}
                placeholder="Telegram Chat ID"
                className="flex-1 bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-xl p-2 text-sm focus:border-green-500 outline-none"
              />
            </div>
            <div className="flex flex-wrap gap-2">
              {watchlist?.map((ticker: string) => (
                <div key={ticker} className="flex items-center gap-2 px-3 py-1 bg-zinc-100 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-full group">
                  <span className="text-xs font-bold">{ticker}</span>
                  <button 
                    onClick={() => removeWatchlistMutation.mutate(ticker)}
                    className="text-zinc-400 hover:text-red-500 transition-colors"
                  >
                    <Trash2 size={12} />
                  </button>
                </div>
              ))}
              {(!watchlist || watchlist.length === 0) && (
                <p className="text-xs text-zinc-400 italic">No stocks in watchlist.</p>
              )}
            </div>
          </div>
        </SettingSection>

        {/* Active Alerts Section */}
        <SettingSection title="Active Indicators Alerts" icon={Bell}>
          <div className="space-y-3">
            {activeAlerts?.filter((a: UserAlert) => a.chatId === chatId).map((alert: UserAlert) => (
              <div key={alert.id} className="flex items-center justify-between p-4 bg-zinc-50 dark:bg-black rounded-2xl border border-zinc-200 dark:border-[#2C2C2E]">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs font-black px-1.5 py-0.5 bg-green-500/10 text-green-500 rounded uppercase">{alert.ticker}</span>
                    <span className="text-sm font-bold">{alert.indicatorName}</span>
                  </div>
                  <p className="text-[10px] text-zinc-500">
                    Condition: {alert.conditionOperator} {alert.targetValue}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                  <span className="text-[10px] font-bold text-green-500 uppercase tracking-tighter">Monitoring</span>
                </div>
              </div>
            ))}
            {(!activeAlerts || activeAlerts.filter((a: UserAlert) => a.chatId === chatId).length === 0) && (
              <p className="text-sm text-zinc-400 italic p-4 text-center">No active alerts for this ID.</p>
            )}
          </div>
        </SettingSection>

        {/* Appearance Setting */}
        <SettingSection title="Appearance" icon={Sun}>
          <div className="grid grid-cols-3 gap-3">
            {[
              { id: 'light', icon: Sun, label: 'Light' },
              { id: 'dark', icon: Moon, label: 'Dark' },
              { id: 'system', icon: Monitor, label: 'System' }
            ].map((t) => (
              <button
                key={t.id}
                onClick={() => setTheme(t.id as any)}
                className={`flex flex-col items-center gap-2 p-4 rounded-2xl border transition-all ${
                  theme === t.id 
                    ? 'bg-green-500/10 border-green-500 text-green-500' 
                    : 'bg-zinc-50 dark:bg-black border-zinc-200 dark:border-[#2C2C2E] hover:border-zinc-400'
                }`}
              >
                <t.icon size={20} />
                <span className="text-xs font-bold uppercase">{t.label}</span>
              </button>
            ))}
          </div>
        </SettingSection>

        {/* Infrastructure Monitor */}
        <SettingSection title="System Status" icon={Cpu}>
          <div className="space-y-3">
            {[
              { name: 'Gateway', status: gatewayStatus ? 'UP' : 'DOWN', version: gatewayStatus?.version },
              { name: 'Corp Service', status: 'CONNECTED', version: '0.0.1' },
              { name: 'Finance Service', status: 'CONNECTED', version: '0.0.1' },
              { name: 'Price Service', status: 'CONNECTED', version: '0.0.1' },
            ].map((s) => (
              <div key={s.name} className="flex items-center justify-between p-4 bg-zinc-50 dark:bg-black rounded-2xl border border-zinc-200 dark:border-[#2C2C2E]">
                <div className="flex items-center gap-3">
                  <div className={`w-2 h-2 rounded-full ${s.status === 'UP' || s.status === 'CONNECTED' ? 'bg-green-500' : 'bg-red-500'}`} />
                  <span className="font-bold text-sm">{s.name}</span>
                </div>
                <span className="text-xs text-zinc-500 font-mono">v{s.version}</span>
              </div>
            ))}
          </div>
        </SettingSection>

        {/* Batch Control */}
        <SettingSection title="Batch Control Center" icon={Database}>
          <div className="space-y-8">
            
            {/* Infrastructure */}
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Infrastructure Sync</label>
                <input 
                  type="text" 
                  value={financeDate}
                  onChange={(e) => setFinanceDate(e.target.value)}
                  placeholder="yyyyMMdd"
                  className="w-32 bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-lg p-1 text-xs focus:border-green-500 outline-none font-mono text-center"
                />
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                <BatchButton label="Corp Info Sync" onClick={() => corpBatch.mutate(financeDate)} isLoading={corpBatch.isPending} />
                <BatchButton label="Sector Update" onClick={() => corpService.runSectorUpdateBatch()} />
              </div>
            </div>

            {/* Price Recovery */}
            <div className="space-y-4 pt-4 border-t border-zinc-100 dark:border-[#2C2C2E]">
              <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase">Stock Price Recovery (Range)</label>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[10px] font-bold text-zinc-400 block mb-1">START DATE (yyyyMMdd)</label>
                  <input 
                    type="text" 
                    value={priceStartDate}
                    onChange={(e) => setPriceStartDate(e.target.value)}
                    className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-xl p-2 text-sm focus:border-green-500 outline-none transition-colors font-mono"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-bold text-zinc-400 block mb-1">END DATE (yyyyMMdd)</label>
                  <input 
                    type="text" 
                    value={priceEndDate}
                    onChange={(e) => setPriceEndDate(e.target.value)}
                    className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-xl p-2 text-sm focus:border-green-500 outline-none transition-colors font-mono"
                  />
                </div>
              </div>
              <BatchButton 
                label="Run Full Price & Aggregate Recovery" 
                onClick={() => priceRecoveryMutation.mutate()} 
                isLoading={priceRecoveryMutation.isPending} 
              />
            </div>

            {/* Finance Bulk Recovery */}
            <div className="space-y-4 pt-4 border-t border-zinc-100 dark:border-[#2C2C2E]">
              <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase">Finance Bulk Recovery (Year Range)</label>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[10px] font-bold text-zinc-400 block mb-1">START YEAR (yyyy)</label>
                  <input 
                    type="text" 
                    value={financeStartYear}
                    onChange={(e) => setFinanceStartYear(e.target.value)}
                    className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-xl p-2 text-sm focus:border-green-500 outline-none transition-colors font-mono"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-bold text-zinc-400 block mb-1">END YEAR (yyyy)</label>
                  <input 
                    type="text" 
                    value={financeEndYear}
                    onChange={(e) => setFinanceEndYear(e.target.value)}
                    className="w-full bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-xl p-2 text-sm focus:border-green-500 outline-none transition-colors font-mono"
                  />
                </div>
              </div>
              <BatchButton 
                label="Run DART Finance Bulk Recovery" 
                onClick={() => financeRecoveryMutation.mutate()} 
                isLoading={financeRecoveryMutation.isPending} 
              />
            </div>

            {/* Finance Single Recovery */}
            <div className="space-y-4 pt-4 border-t border-zinc-100 dark:border-[#2C2C2E]">
              <div className="flex justify-between items-center">
                <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase">Finance Single Sync (Year: {financeYear})</label>
                <input 
                  type="text" 
                  value={financeYear}
                  onChange={(e) => setFinanceYear(e.target.value)}
                  className="w-20 bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-lg p-1 text-xs focus:border-green-500 outline-none font-mono"
                />
              </div>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                <BatchButton label="Q1" onClick={() => financeBatch.mutate('Q1')} isLoading={financeBatch.isPending && financeBatch.variables === 'Q1'} />
                <BatchButton label="SEMI" onClick={() => financeBatch.mutate('SEMI')} isLoading={financeBatch.isPending && financeBatch.variables === 'SEMI'} />
                <BatchButton label="Q3" onClick={() => financeBatch.mutate('Q3')} isLoading={financeBatch.isPending && financeBatch.variables === 'Q3'} />
                <BatchButton label="ANNUAL" onClick={() => financeBatch.mutate('ANNUAL')} isLoading={financeBatch.isPending && financeBatch.variables === 'ANNUAL'} />
              </div>
            </div>

          </div>
        </SettingSection>

        {/* Strategy Defaults */}
        <SettingSection title="Strategy Defaults" icon={ShieldCheck}>
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-sm font-medium text-zinc-500">Trading Fee Rate</span>
              <input 
                type="number" 
                value={defaults.tradingFeeRate}
                onChange={(e) => setDefaults({ ...defaults, tradingFeeRate: parseFloat(e.target.value) })}
                className="w-24 bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-lg p-2 text-right outline-none"
              />
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm font-medium text-zinc-500">Tax Rate</span>
              <input 
                type="number" 
                value={defaults.taxRate}
                onChange={(e) => setDefaults({ ...defaults, taxRate: parseFloat(e.target.value) })}
                className="w-24 bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-lg p-2 text-right outline-none"
              />
            </div>
          </div>
        </SettingSection>
      </div>
    </div>
  );
}
