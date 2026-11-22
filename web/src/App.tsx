import React, { useState, useMemo } from 'react';
import { SimulationConfig, runSimulation } from './engine/dsp';
import { OscilloscopeView } from './components/OscilloscopeView';
import { NoiseAnalysisView } from './components/NoiseAnalysisView';
import { BerCurvesView } from './components/BerCurvesView';
import { MultipathHeatmapView } from './components/MultipathHeatmapView';
import { ClientCaseStudiesView } from './components/ClientCaseStudiesView';
import {
  Activity,
  BarChart3,
  TrendingDown,
  Grid,
  Briefcase,
  Radio,
  Cpu,
} from 'lucide-react';

type ActiveTab = 'oscilloscope' | 'bruit' | 'teb' | 'multitrajets' | 'tp6';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ActiveTab>('oscilloscope');

  const [config, setConfig] = useState<SimulationConfig>({
    messageMode: 'fixed',
    fixedMessage: '1011001011010010',
    nbBits: 1000,
    useSeed: true,
    seed: 42,
    analogMode: true,
    form: 'NRZ',
    nbEch: 30,
    amplMin: -1.0,
    amplMax: 1.0,
    useSnr: true,
    snrpbDb: 8.0,
    echoes: [{ id: 'init-1', dt: 30, ar: 0.35 }],
    useCodeur: false,
  });

  const simResult = useMemo(() => runSimulation(config), [config]);

  const handleLoadIntoOscilloscope = (patch: Partial<SimulationConfig>) => {
    setConfig((prev) => ({
      ...prev,
      ...patch,
    }));
    setActiveTab('oscilloscope');
  };

  const tabs: {
    id: ActiveTab;
    label: string;
    badge: string;
    icon: React.ReactNode;
  }[] = [
    {
      id: 'oscilloscope',
      label: 'Oscilloscope & Sondes',
      badge: 'TP1–TP5',
      icon: <Activity className="w-4 h-4" />,
    },
    {
      id: 'tp6',
      label: 'Études Client (Env. 1 & 2)',
      badge: 'TP6',
      icon: <Briefcase className="w-4 h-4" />,
    },
    {
      id: 'teb',
      label: 'Courbes TEB & Codage',
      badge: 'TP3 / TP5',
      icon: <TrendingDown className="w-4 h-4" />,
    },
    {
      id: 'multitrajets',
      label: 'Cartes Multitrajets 2D',
      badge: 'TP4',
      icon: <Grid className="w-4 h-4" />,
    },
    {
      id: 'bruit',
      label: 'Bruit Box-Muller & Q-Q',
      badge: 'TP3',
      icon: <BarChart3 className="w-4 h-4" />,
    },
  ];

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* HEADER PRINCIPAL */}
      <header className="border-b border-slate-800/90 bg-slate-900/80 backdrop-blur sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500 to-emerald-500 flex items-center justify-center shadow-lg shadow-cyan-500/20">
              <Radio className="w-5 h-5 text-slate-950 stroke-[2.4]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base sm:text-lg font-bold tracking-tight text-white">
                  SIT213 · Digital Transmission Studio
                </h1>
                <span className="px-2 py-0.5 text-[10px] font-mono font-bold uppercase rounded-full bg-cyan-500/15 text-cyan-300 border border-cyan-500/30">
                  IMT Atlantique · FIP 2A
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Simulateur interactif de chaîne de transmission numérique (NRZ / RZ / NRZT · BBAG · Multitrajets · Codeur 1/3)
              </p>
            </div>
          </div>

          {/* Schéma synoptique dynamique de la chaîne active */}
          <div className="hidden xl:flex items-center gap-1.5 text-[11px] font-mono bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800">
            <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-200">
              Source ({simResult.sourceBits.length}b)
            </span>
            <span className="text-slate-600">&rarr;</span>
            {config.useCodeur && (
              <>
                <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  Codeur 1/3 ({simResult.codedBits.length}b)
                </span>
                <span className="text-slate-600">&rarr;</span>
              </>
            )}
            <span className="px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">
              {config.form} [{config.amplMin},{config.amplMax}]
            </span>
            <span className="text-slate-600">&rarr;</span>
            <span className="px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">
              Canal ({config.useSnr ? `${config.snrpbDb}dB` : 'Parfait'}
              {config.echoes.length > 0 ? ` + ${config.echoes.length} écho` : ''})
            </span>
            <span className="text-slate-600">&rarr;</span>
            <span className="px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
              TEB = {simResult.usefulTeb === 0 ? '0.00' : simResult.usefulTeb.toExponential(2)}
            </span>
          </div>
        </div>

        {/* BARRE D'ONGLETS DE NAVIGATION */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 flex items-center gap-1.5 overflow-x-auto pb-2 pt-1">
          {tabs.map((t) => {
            const isActive = activeTab === t.id;
            return (
              <button
                key={t.id}
                onClick={() => setActiveTab(t.id)}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-semibold whitespace-nowrap transition border ${
                  isActive
                    ? 'bg-cyan-500/15 text-cyan-300 border-cyan-500/50 shadow-sm'
                    : 'bg-slate-900/40 text-slate-400 border-transparent hover:text-slate-200 hover:bg-slate-800/60'
                }`}
              >
                {t.icon}
                <span>{t.label}</span>
                <span
                  className={`px-1.5 py-0.2 rounded text-[10px] font-mono ${
                    isActive
                      ? 'bg-cyan-500/25 text-cyan-200'
                      : 'bg-slate-800 text-slate-400'
                  }`}
                >
                  {t.badge}
                </span>
              </button>
            );
          })}
        </div>
      </header>

      {/* CONTENU PRINCIPAL */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6">
        {activeTab === 'oscilloscope' && (
          <OscilloscopeView
            config={config}
            setConfig={setConfig}
            result={simResult}
          />
        )}
        {activeTab === 'tp6' && (
          <ClientCaseStudiesView
            onLoadIntoOscilloscope={handleLoadIntoOscilloscope}
          />
        )}
        {activeTab === 'teb' && <BerCurvesView />}
        {activeTab === 'multitrajets' && (
          <MultipathHeatmapView
            onLoadIntoOscilloscope={handleLoadIntoOscilloscope}
          />
        )}
        {activeTab === 'bruit' && <NoiseAnalysisView />}
      </main>

      {/* FOOTER */}
      <footer className="border-t border-slate-900 bg-slate-950/90 py-4 px-4 sm:px-6 text-xs text-slate-500">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Cpu className="w-3.5 h-3.5 text-cyan-500" />
            <span>
              SIT213 · Simulation d'un système de transmission numérique — Nathan Weymeels, Antonin Pons, Amandine Touchais, Aymeric Schaeffer
            </span>
          </div>
          <div className="font-mono text-[11px] text-slate-400">
            Moteur DSP TypeScript conforme bit-à-bit au simulateur Java 17 (TP1 à TP6)
          </div>
        </div>
      </footer>
    </div>
  );
};

export default App;
