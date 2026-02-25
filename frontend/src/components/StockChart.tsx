import React, { useEffect, useRef } from 'react';
import { 
  createChart, 
  ColorType, 
  IChartApi, 
  CandlestickSeries, 
  AreaSeries, 
  LineSeries 
} from 'lightweight-charts';
import { PriceData, calculateMA } from '../utils/chartUtils';
import { useSettingsStore } from '../hooks/useSettingsStore';

interface StockChartProps {
  data: PriceData[];
  mainColor: string;
}

export default function StockChart({ data, mainColor }: StockChartProps) {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const { theme, chartConfig } = useSettingsStore();

  useEffect(() => {
    if (!chartContainerRef.current) return;

    const isDark = theme === 'dark' || (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);

    const chart = createChart(chartContainerRef.current, {
      layout: {
        background: { type: ColorType.Solid, color: 'transparent' },
        textColor: isDark ? '#8E8E93' : '#555',
      },
      grid: {
        vertLines: { visible: false },
        horzLines: { color: isDark ? '#1E1E1E' : '#F0F0F0' },
      },
      width: chartContainerRef.current.clientWidth || 800,
      height: 450,
      timeScale: {
        borderVisible: false,
      },
      rightPriceScale: {
        borderVisible: false,
      },
      crosshair: {
        horzLine: { visible: chartConfig.isCandleMode },
        vertLine: { labelBackgroundColor: mainColor },
      }
    });

    chartRef.current = chart;

    const updateData = () => {
      if (!data || data.length === 0) return;

      try {
        if (chartConfig.isCandleMode) {
          const candleSeries = chart.addSeries(CandlestickSeries, {
            upColor: isDark ? '#00C805' : '#16a34a',
            downColor: isDark ? '#FF5000' : '#dc2626',
            borderVisible: false,
            wickUpColor: isDark ? '#00C805' : '#16a34a',
            wickDownColor: isDark ? '#FF5000' : '#dc2626',
          });
          candleSeries.setData(data as any);
        } else {
          const areaSeries = chart.addSeries(AreaSeries, {
            lineColor: mainColor,
            topColor: `${mainColor}33`,
            bottomColor: `${mainColor}00`,
            lineWidth: 3,
          });
          areaSeries.setData(data.map((d: any) => ({ time: d.time, value: d.close })));
        }

        // Moving Averages
        const maConfigs = [
          { id: 'showMA5', period: 5, color: '#3B82F6' },
          { id: 'showMA20', period: 20, color: '#F59E0B' },
          { id: 'showMA60', period: 60, color: '#8B5CF6' },
          { id: 'showMA120', period: 120, color: '#EC4899' },
        ];

        maConfigs.forEach(cfg => {
          if ((chartConfig as any)[cfg.id]) {
            try {
              const maData = calculateMA(data, cfg.period);
              const maSeries = chart.addSeries(LineSeries, {
                color: cfg.color,
                lineWidth: 2,
                priceLineVisible: false,
                lastValueVisible: false,
              });
              maSeries.setData(maData as any);
            } catch (maErr) {
              console.error(`Error setting MA ${cfg.period}:`, maErr);
            }
          }
        });

        chart.timeScale().fitContent();
      } catch (err) {
        console.error('Error setting chart data:', err);
      }
    };

    updateData();

    const handleResize = () => {
      if (chartContainerRef.current && chartRef.current) {
        chartRef.current.applyOptions({ 
          width: chartContainerRef.current.clientWidth,
        });
      }
    };

    const resizeObserver = new ResizeObserver(handleResize);
    resizeObserver.observe(chartContainerRef.current);

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      resizeObserver.disconnect();
      chart.remove();
    };
  }, [data, theme, chartConfig, mainColor]);

  return <div ref={chartContainerRef} className="w-full h-full" />;
}
