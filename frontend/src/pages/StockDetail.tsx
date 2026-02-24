import React, { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { TrendingUp, TrendingDown, Info, BarChart3, RefreshCw } from 'lucide-react';
import { corpService, priceService, financeService } from '../services/api';
import { aggregateData } from '../utils/chartUtils';
import { formatCurrency, formatPercent } from '../utils/cn';
import { useSettingsStore } from '../hooks/useSettingsStore';
import StockChart from '../components/StockChart';

export default function StockDetail() {
  const { symbol } = useParams();
  const [timeframe, setTimeframe] = useState<'D' | 'W' | 'M'>('D');
  const [lookbackDays, setLookbackDays] = useState(730);
  const { theme, chartConfig, setChartConfig } = useSettingsStore();

  const { data: corp, isLoading: isLoadingCorp } = useQuery({
    queryKey: ['corp', symbol],
    queryFn: () => corpService.getCorpDetail(symbol!),
    enabled: !!symbol,
  });

  const { data: rawPriceData, isLoading: isLoadingPrice } = useQuery({
    queryKey: ['prices', symbol, lookbackDays],
    queryFn: () => priceService.getPriceHistory(symbol!, lookbackDays),
    enabled: !!symbol,
  });

  const { data: finance, isLoading: isLoadingFinance } = useQuery({
    queryKey: ['finance', symbol],
    queryFn: () => financeService.getFinanceSummary(symbol!),
    enabled: !!symbol,
  });

  const processedData = useMemo(() => {
    if (!rawPriceData || rawPriceData.length === 0) return [];
    const baseData = rawPriceData.map((d: any) => ({
      time: d.basDt,
      open: d.startPrice,
      high: d.highPrice,
      low: d.lowPrice,
      close: d.endPrice,
      value: d.volume,
    }));
    return aggregateData(baseData, timeframe);
  }, [rawPriceData, timeframe]);

  if (isLoadingCorp || isLoadingPrice) {
    return (
      <div className="flex flex-col items-center justify-center h-[60vh] space-y-4">
        <RefreshCw size={48} className="text-green-500 animate-spin" />
        <p className="text-zinc-500 font-bold">Loading market data...</p>
      </div>
    );
  }

  if (processedData.length === 0 && !isLoadingPrice) {
    return (
      <div className="flex flex-col items-center justify-center h-[60vh] space-y-4">
        <Info size={48} className="text-zinc-300" />
        <h2 className="text-xl font-bold">No Price Data Found</h2>
        <p className="text-zinc-500">Please run the Price Batch in Settings for {symbol}.</p>
      </div>
    );
  }

  const latestPrice = processedData.length > 0 ? processedData[processedData.length - 1].close : 0;
  const prevPrice = processedData.length > 1 ? processedData[processedData.length - 2].close : latestPrice;
  const priceDiff = latestPrice - prevPrice;
  const priceRatio = prevPrice !== 0 ? (priceDiff / prevPrice) * 100 : 0;
  const isPositive = priceDiff >= 0;
  const mainColor = isPositive ? (theme === 'dark' ? '#00C805' : '#16a34a') : (theme === 'dark' ? '#FF5000' : '#dc2626');

  return (
    <div className="max-w-5xl mx-auto space-y-10 pb-20 animate-in fade-in duration-700">
      {/* Hero Section */}
      <section className="space-y-2">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-4xl font-black tracking-tight">{corp?.corpName || symbol}</h1>
            <p className="text-zinc-500 font-bold uppercase tracking-widest text-sm">{corp?.market} · {symbol}</p>
          </div>
          <div className="flex gap-2">
            <button 
              onClick={() => setChartConfig({ isCandleMode: !chartConfig.isCandleMode })}
              className={`p-2 rounded-lg border transition-all ${chartConfig.isCandleMode ? 'bg-green-500/10 border-green-500 text-green-600' : 'bg-white dark:bg-[#111] border-zinc-200 dark:border-zinc-800'}`}
            >
              <BarChart3 size={20} />
            </button>
          </div>
        </div>
        
        <div className="pt-4">
          <h2 className="text-6xl font-black tracking-tighter">
            {formatCurrency(latestPrice)}
          </h2>
          <div className={`flex items-center gap-2 font-bold text-lg ${isPositive ? 'text-green-600 dark:text-[#00C805]' : 'text-red-600 dark:text-[#FF5000]'}`}>
            {isPositive ? <TrendingUp size={20} /> : <TrendingDown size={20} />}
            <span>{formatCurrency(Math.abs(priceDiff))} ({formatPercent(priceRatio)})</span>
            <span className="text-zinc-400 text-sm ml-1 uppercase">Today</span>
          </div>
        </div>
      </section>

      {/* Main Chart Area */}
      <section className="space-y-6">
        <div className="h-[450px] w-full relative">
          <StockChart data={processedData} mainColor={mainColor} />
        </div>
        
        <div className="flex flex-wrap justify-between items-center border-b border-zinc-100 dark:border-zinc-900 pb-4 gap-4">
          <div className="flex flex-wrap gap-4">
            <div className="flex gap-1 bg-zinc-100 dark:bg-zinc-900 p-1 rounded-xl">
              {[
                { label: '1D', value: 'D' },
                { label: '1W', value: 'W' },
                { label: '1M', value: 'M' }
              ].map((t) => (
                <button
                  key={t.label}
                  onClick={() => setTimeframe(t.value as any)}
                  className={`px-4 py-1.5 rounded-lg font-black text-xs transition-all ${
                    timeframe === t.value 
                      ? 'text-green-600 dark:text-[#00C805] bg-white dark:bg-black shadow-sm' 
                      : 'text-zinc-400 hover:text-zinc-600 dark:hover:white'
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>

            <div className="flex gap-1 bg-zinc-100 dark:bg-zinc-900 p-1 rounded-xl">
              {[
                { label: '30D', value: 30 },
                { label: '1Y', value: 365 },
                { label: '2Y', value: 730 },
                { label: '5Y', value: 1825 }
              ].map((d) => (
                <button
                  key={d.label}
                  onClick={() => setLookbackDays(d.value)}
                  className={`px-3 py-1.5 rounded-lg font-black text-xs transition-all ${
                    lookbackDays === d.value 
                      ? 'text-blue-600 bg-white dark:bg-black shadow-sm' 
                      : 'text-zinc-400 hover:text-zinc-600 dark:hover:white'
                  }`}
                >
                  {d.label}
                </button>
              ))}
            </div>
          </div>
          
          <div className="flex gap-4 items-center overflow-x-auto no-scrollbar px-2">
             {[
               { id: 'showMA5', label: 'MA5', color: 'bg-blue-500' },
               { id: 'showMA20', label: 'MA20', color: 'bg-amber-500' },
               { id: 'showMA60', label: 'MA60', color: 'bg-purple-500' },
               { id: 'showMA120', label: 'MA120', color: 'bg-pink-500' },
             ].map((ma) => (
               <button
                 key={ma.id}
                 onClick={() => setChartConfig({ [ma.id]: !((chartConfig as any)[ma.id]) })}
                 className={`flex items-center gap-2 px-3 py-1 rounded-full border text-[10px] font-black transition-all ${
                   (chartConfig as any)[ma.id]
                     ? `border-transparent ${ma.color} text-white`
                     : 'border-zinc-200 dark:border-zinc-800 text-zinc-400'
                 }`}
               >
                 {ma.label}
               </button>
             ))}
          </div>
        </div>
      </section>

      {/* Key Stats Grid */}
      <section className="space-y-6">
        <h3 className="text-2xl font-black">Key Statistics</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-y-8 gap-x-12">
          {[
            { label: 'Market Cap', value: finance?.marketCap ? `${(finance.marketCap / 1e12).toFixed(2)}T` : '-' },
            { label: 'Price-Earnings Ratio', value: finance?.per || '-' },
            { label: 'Dividend Yield', value: finance?.dividendYield ? `${finance.dividendYield}%` : '-' },
            { label: 'Average Volume', value: corp?.avgVolume?.toLocaleString() || '-' },
            { label: '52 Week High', value: formatCurrency(corp?.high52w || 0) },
            { label: '52 Week Low', value: formatCurrency(corp?.low52w || 0) },
            { label: 'ROE', value: `${finance?.roe || '-'}%` },
            { label: 'PBR', value: finance?.pbr || '-' },
          ].map((stat) => (
            <div key={stat.label} className="space-y-1">
              <p className="text-zinc-500 dark:text-[#8E8E93] text-sm font-bold">{stat.label}</p>
              <p className="text-xl font-black">{stat.value}</p>
            </div>
          ))}
        </div>
      </section>

      {/* About Section */}
      <section className="space-y-4 max-w-3xl">
        <h3 className="text-2xl font-black">About</h3>
        <p className="text-zinc-600 dark:text-zinc-400 leading-relaxed font-medium">
          {corp?.description || 'No description available for this company.'}
        </p>
        <div className="flex flex-wrap gap-2 pt-2">
          <span className="px-4 py-2 bg-zinc-100 dark:bg-zinc-900 rounded-full text-xs font-bold text-zinc-500">{corp?.sector}</span>
          <span className="px-4 py-2 bg-zinc-100 dark:bg-zinc-900 rounded-full text-xs font-bold text-zinc-500">{corp?.corpType}</span>
        </div>
      </section>
    </div>
  );
}
