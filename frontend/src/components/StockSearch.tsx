import React, { useState, useEffect, useRef } from 'react';
import { Search, Loader2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { corpService } from '../services/api';

export default function StockSearch() {
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const navigate = useNavigate();
  const containerRef = useRef<HTMLDivElement>(null);

  const { data: results, isLoading } = useQuery({
    queryKey: ['search', query],
    queryFn: () => corpService.searchCorps(query),
    enabled: query.length >= 2,
  });

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (symbol: string) => {
    navigate(`/stock/${symbol}`);
    setQuery('');
    setIsOpen(false);
  };

  return (
    <div ref={containerRef} className="relative w-full max-w-md">
      <div className="relative">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" size={18} />
        <input
          type="text"
          placeholder="Search stocks (e.g. 005930, Samsung)"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setIsOpen(true);
          }}
          onFocus={() => setIsOpen(true)}
          className="w-full bg-zinc-100 dark:bg-zinc-900 border-none rounded-full py-3 pl-12 pr-4 text-sm font-bold focus:ring-2 focus:ring-green-500 transition-all outline-none"
        />
        {isLoading && <Loader2 className="absolute right-4 top-1/2 -translate-y-1/2 animate-spin text-green-500" size={18} />}
      </div>

      {isOpen && query.length >= 2 && results && (
        <div className="absolute top-full mt-2 w-full bg-white dark:bg-[#111] border border-zinc-200 dark:border-zinc-800 rounded-2xl shadow-xl z-50 overflow-hidden overflow-y-auto max-h-96">
          {results.length > 0 ? (
            results.map((item: any) => (
              <button
                key={item.stockCode}
                onClick={() => handleSelect(item.stockCode)}
                className="w-full flex items-center justify-between p-4 hover:bg-zinc-50 dark:hover:bg-zinc-900 transition-colors border-b border-zinc-100 dark:border-zinc-900 last:border-none"
              >
                <div className="text-left">
                  <p className="font-black text-sm">{item.corpName}</p>
                  <p className="text-xs text-zinc-500 font-bold">{item.stockCode}</p>
                </div>
                <span className="text-[10px] font-black bg-zinc-100 dark:bg-zinc-800 px-2 py-1 rounded text-zinc-500 uppercase">{item.market}</span>
              </button>
            ))
          ) : (
            <div className="p-8 text-center text-zinc-500 font-bold text-sm">No results found.</div>
          )}
        </div>
      )}
    </div>
  );
}
