import React, { useState } from 'react';
import { Sun, Moon, Monitor, Server, Play, ShieldCheck, Database, RefreshCw, Cpu } from 'lucide-react';
import { useSettingsStore } from '../hooks/useSettingsStore';
import { useQuery, useMutation } from '@tanstack/react-query';
import { systemService, corpService, financeService, priceService } from '../services/api';

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
  const { theme, setTheme, defaults, setDefaults } = useSettingsStore();
  
  // Batch Parameters States
  const [priceStartDate, setPriceStartDate] = useState('20241014');
  const [priceEndDate, setPriceEndDate] = useState('20241014');
  const [financeStartYear, setFinanceStartYear] = useState('2023');
  const [financeEndYear, setFinanceEndYear] = useState('2023');
  const [financeDate, setFinanceDate] = useState('20241014');

  // Service Health Queries
  const { data: gatewayStatus } = useQuery({ queryKey: ['health', 'gateway'], queryFn: () => systemService.getServiceInfo() });

  // Batch Mutations
  const corpBatch = useMutation({ mutationFn: (date: string) => corpService.runCorpInfoBatch(date) });
  const priceRecoveryMutation = useMutation({ 
    mutationFn: () => priceService.runPriceRecovery(priceStartDate, priceEndDate) 
  });
  const financeRecoveryMutation = useMutation({ 
    mutationFn: () => financeService.runFinanceRecovery(financeStartYear, financeEndYear) 
  });
  const financeBatch = useMutation({ 
    mutationFn: (code: string) => financeService.runFinanceBatch(financeDate, code) 
  });

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700 pb-20">
      <header>
        <h2 className="text-3xl md:text-4xl font-black mb-2">Settings</h2>
        <p className="text-zinc-500 dark:text-[#8E8E93]">System configuration and infrastructure control.</p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
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
            <div className="space-y-3">
              <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase tracking-wider">Infrastructure Sync (Date: {financeDate})</label>
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
                <label className="text-xs font-bold text-zinc-500 dark:text-[#8E8E93] uppercase">Finance Single Sync (Date: {financeDate})</label>
                <input 
                  type="text" 
                  value={financeDate}
                  onChange={(e) => setFinanceDate(e.target.value)}
                  className="w-28 bg-zinc-50 dark:bg-black border border-zinc-200 dark:border-[#2C2C2E] rounded-lg p-1 text-xs focus:border-green-500 outline-none font-mono"
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
