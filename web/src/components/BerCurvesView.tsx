import React, { useState, useMemo } from 'react';
import {
  ENV1_REFERENCE_DATA,
  dbToLin,
  tebTheoNrzAntipolaire,
  tebTheoUnipolaire,
  tebTheoNrztAntipolaire,
  tebTheoAvecCodeurUtile,
  tebTheoAvecCodeurCanal,
  runSimulation,
} from '../engine/dsp';
import { TrendingDown, Play, Layers } from 'lucide-react';

export const BerCurvesView: React.FC = () => {
  const [energyMode, setEnergyMode] = useState<'useful' | 'channel'>('useful');
  const [liveNbBits, setLiveNbBits] = useState(15000);
  const [liveResults, setLiveResults] = useState<Record<string, number[]> | null>(null);
  const [isRunning, setIsRunning] = useState(false);

  const snrGrid = ENV1_REFERENCE_DATA.snrs; // 0..10 dB

  const runLiveMonteCarlo = () => {
    setIsRunning(true);
    setTimeout(() => {
      const res: Record<string, number[]> = {
        'NRZ antipolaire [-1, 1]': [],
        'RZ [0, 1]': [],
        'NRZT antipolaire [-1, 1]': [],
        'NRZ + Codeur TP5': [],
      };

      for (const snr of snrGrid) {
        const rNrz = runSimulation({
          messageMode: 'random',
          nbBits: liveNbBits,
          fixedMessage: '10110010',
          useSeed: true,
          seed: 42,
          analogMode: true,
          form: 'NRZ',
          nbEch: 30,
          amplMin: -1,
          amplMax: 1,
          useSnr: true,
          snrpbDb: snr,
          echoes: [],
          useCodeur: false,
        });
        res['NRZ antipolaire [-1, 1]'].push(rNrz.usefulTeb);

        const rRz = runSimulation({
          messageMode: 'random',
          nbBits: liveNbBits,
          fixedMessage: '10110010',
          useSeed: true,
          seed: 42,
          analogMode: true,
          form: 'RZ',
          nbEch: 30,
          amplMin: 0,
          amplMax: 1,
          useSnr: true,
          snrpbDb: snr,
          echoes: [],
          useCodeur: false,
        });
        res['RZ [0, 1]'].push(rRz.usefulTeb);

        const rNrzt = runSimulation({
          messageMode: 'random',
          nbBits: liveNbBits,
          fixedMessage: '10110010',
          useSeed: true,
          seed: 42,
          analogMode: true,
          form: 'NRZT',
          nbEch: 30,
          amplMin: -1,
          amplMax: 1,
          useSnr: true,
          snrpbDb: snr,
          echoes: [],
          useCodeur: false,
        });
        res['NRZT antipolaire [-1, 1]'].push(rNrzt.usefulTeb);

        const snrCanal =
          energyMode === 'useful' ? snr - 10 * Math.log10(3) : snr;
        const rCod = runSimulation({
          messageMode: 'random',
          nbBits: Math.max(3000, Math.floor(liveNbBits / 2)),
          fixedMessage: '10110010',
          useSeed: true,
          seed: 42,
          analogMode: true,
          form: 'NRZ',
          nbEch: 30,
          amplMin: -1,
          amplMax: 1,
          useSnr: true,
          snrpbDb: snrCanal,
          echoes: [],
          useCodeur: true,
        });
        res['NRZ + Codeur TP5'].push(rCod.usefulTeb);
      }

      setLiveResults(res);
      setIsRunning(false);
    }, 30);
  };

  // Coordonnées SVG du graphique logarithmique TEB vs SNR
  const chart = useMemo(() => {
    const W = 760;
    const H = 400;
    const padL = 65;
    const padR = 30;
    const padT = 25;
    const padB = 45;
    const plotW = W - padL - padR;
    const plotH = H - padT - padB;

    const xMin = 0;
    const xMax = 10;
    const logYMin = -5; // 10^-5
    const logYMax = 0;  // 10^0

    const toX = (snr: number) => padL + ((snr - xMin) / (xMax - xMin)) * plotW;
    const toY = (teb: number) => {
      const clamped = Math.max(1e-5, Math.min(1, teb));
      const logV = Math.log10(clamped);
      return padT + ((logYMax - logV) / (logYMax - logYMin)) * plotH;
    };

    // Courbes théoriques continues (101 points de 0 à 10 dB)
    const xsFine = Array.from({ length: 101 }, (_, i) => i * 0.1);
    const buildCurvePath = (fn: (lin: number) => number) =>
      xsFine
        .map((x, idx) => {
          const y = fn(dbToLin(x));
          return `${idx === 0 ? 'M' : 'L'} ${toX(x).toFixed(1)} ${toY(y).toFixed(1)}`;
        })
        .join(' ');

    return {
      W,
      H,
      padL,
      padR,
      padT,
      padB,
      plotW,
      plotH,
      toX,
      toY,
      theoNrzPath: buildCurvePath(tebTheoNrzAntipolaire),
      theoUnipolPath: buildCurvePath(tebTheoUnipolaire),
      theoNrztPath: buildCurvePath((l) => tebTheoNrztAntipolaire(l, 30)),
      theoCodeurPath: buildCurvePath(
        energyMode === 'useful' ? tebTheoAvecCodeurUtile : tebTheoAvecCodeurCanal
      ),
    };
  }, [energyMode]);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
      {/* PANNEAU GAUCHE : OPTIONS & THÉORIE */}
      <div className="lg:col-span-4 space-y-4">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg space-y-4">
          <h3 className="text-sm font-bold text-cyan-400 flex items-center gap-2">
            <TrendingDown className="w-4 h-4" /> TP3 & TP5 · Comparaison des Courbes TEB
          </h3>

          <div>
            <label className="block text-xs text-slate-400 mb-1.5">
              Convention d'énergie en abscisse (Impact du Codeur TP5)
            </label>
            <div className="grid grid-cols-1 gap-2">
              <button
                onClick={() => {
                  setEnergyMode('useful');
                  setLiveResults(null);
                }}
                className={`text-left p-3 rounded-lg border transition ${
                  energyMode === 'useful'
                    ? 'bg-cyan-500/15 border-cyan-400 text-cyan-200'
                    : 'bg-slate-950 border-slate-800 text-slate-400'
                }`}
              >
                <div className="text-xs font-bold">
                  1. Eb/N0 par bit UTILE (Bilan énergétique TP6 Q3)
                </div>
                <div className="text-[11px] opacity-80 mt-0.5">
                  Tient compte des 3 bits émis par bit utile (-4.77 dB par bit canal). Montre le surcoût net de +1.37 dB (+37 % d'énergie) à TEB = 10⁻³.
                </div>
              </button>

              <button
                onClick={() => {
                  setEnergyMode('channel');
                  setLiveResults(null);
                }}
                className={`text-left p-3 rounded-lg border transition ${
                  energyMode === 'channel'
                    ? 'bg-emerald-500/15 border-emerald-400 text-emerald-200'
                    : 'bg-slate-950 border-slate-800 text-slate-400'
                }`}
              >
                <div className="text-xs font-bold">
                  2. Ec/N0 par bit de CANAL (-snrpb brut TP5)
                </div>
                <div className="text-[11px] opacity-80 mt-0.5">
                  Même réglage -snrpb avec et sans codeur. Montre le gain apparent de +3.40 dB sur le canal (seuil à 3.39 dB au lieu de 6.79 dB).
                </div>
              </button>
            </div>
          </div>

          {/* Campagne Monte-Carlo live dans le navigateur */}
          <div className="pt-3 border-t border-slate-800 space-y-3">
            <div className="flex justify-between text-xs">
              <span className="text-slate-300">Bits / point (Monte-Carlo live)</span>
              <span className="font-mono text-cyan-300 font-bold">
                {liveNbBits.toLocaleString()} bits
              </span>
            </div>
            <input
              type="range"
              min={2000}
              max={40000}
              step={2000}
              value={liveNbBits}
              onChange={(e) => setLiveNbBits(Number(e.target.value))}
              className="w-full accent-cyan-400"
            />
            <button
              onClick={runLiveMonteCarlo}
              disabled={isRunning}
              className="w-full py-2.5 px-4 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold text-xs flex items-center justify-center gap-2 transition shadow-md disabled:opacity-50"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              {isRunning
                ? 'Simulation Monte-Carlo en cours...'
                : 'Simuler les 44 points en direct dans le navigateur'}
            </button>
          </div>
        </div>

        {/* Encadré formules théoriques exactes */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg space-y-2.5 text-xs">
          <div className="font-semibold text-slate-200 flex items-center gap-1.5">
            <Layers className="w-3.5 h-3.5 text-cyan-400" /> Pénalités théoriques face au NRZ [-1, 1]
          </div>
          <div className="p-2.5 rounded bg-slate-950 border border-slate-800 space-y-1.5 font-mono text-[11px]">
            <div className="text-cyan-300">
              • NRZ [-1, 1] : TEB = Q(√(2 Eb/N0)) &rarr; 6.79 dB @ 10⁻³
            </div>
            <div className="text-emerald-300">
              • NRZ [0, 1] & RZ [0, 1] : TEB = Q(√(Eb/N0)) &rarr; +3.01 dB
            </div>
            <div className="text-rose-300">
              • NRZT [-1, 1] : Ps ≈ 0.778 A², tiers central &rarr; +3.68 dB
            </div>
            <div className="text-amber-300">
              • NRZ + Codeur (1/3) : 3p² - 2p³ &rarr; 8.16 dB utile (+1.37 dB)
            </div>
          </div>
        </div>
      </div>

      {/* GRAPHIQUE PRINCIPAL WATERFALL LOGARITHMIQUE */}
      <div className="lg:col-span-8 bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg flex flex-col justify-between">
        <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
          <div>
            <h3 className="text-base font-bold text-slate-100">
              Taux d'Erreur Binaire (TEB) en fonction de{' '}
              {energyMode === 'useful'
                ? 'Eb/N0 par bit utile (dB)'
                : 'Ec/N0 par bit de canal (dB)'}
            </h3>
            <p className="text-xs text-slate-400">
              Traits pleins : lois analytiques exactes · Marqueurs : mesures Java de référence (100 000 bits, graine 42)
              {liveResults ? ' + points Monte-Carlo live' : ''}
            </p>
          </div>
          <span className="px-2.5 py-1 rounded-full text-xs font-mono bg-rose-500/15 text-rose-300 border border-rose-500/30">
            Cible TEB = 10⁻³
          </span>
        </div>

        <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
          <svg viewBox={`0 0 ${chart.W} ${chart.H}`} className="w-full h-96">
            {/* Grille horizontale logarithmique 10^0 à 10^-5 */}
            {[0, -1, -2, -3, -4, -5].map((exp) => {
              const y = chart.toY(Math.pow(10, exp));
              return (
                <g key={exp}>
                  <line
                    x1={chart.padL}
                    y1={y}
                    x2={chart.W - chart.padR}
                    y2={y}
                    stroke={exp === -3 ? '#f43f5e' : '#1e293b'}
                    strokeDasharray={exp === -3 ? '6 4' : undefined}
                    strokeWidth={exp === -3 ? 1.6 : 1}
                  />
                  <text
                    x={chart.padL - 10}
                    y={y + 4}
                    textAnchor="end"
                    fill={exp === -3 ? '#fda4af' : '#94a3b8'}
                    fontSize="11"
                    fontFamily="monospace"
                  >
                    10^{exp}
                  </text>
                </g>
              );
            })}

            {/* Grille verticale SNR 0 à 10 dB */}
            {snrGrid.map((snr) => {
              const x = chart.toX(snr);
              return (
                <g key={snr}>
                  <line
                    x1={x}
                    y1={chart.padT}
                    x2={x}
                    y2={chart.H - chart.padB}
                    stroke="#1e293b"
                  />
                  <text
                    x={x}
                    y={chart.H - chart.padB + 18}
                    textAnchor="middle"
                    fill="#94a3b8"
                    fontSize="11"
                    fontFamily="monospace"
                  >
                    {snr} dB
                  </text>
                </g>
              );
            })}

            {/* Repères verticaux des seuils à TEB = 10^-3 */}
            <line
              x1={chart.toX(6.79)}
              y1={chart.padT}
              x2={chart.toX(6.79)}
              y2={chart.H - chart.padB}
              stroke="#38bdf8"
              strokeDasharray="3 3"
              strokeWidth={1.3}
            />
            <rect
              x={chart.toX(6.79) - 54}
              y={chart.H - chart.padB - 32}
              width={50}
              height={20}
              rx={4}
              fill="#0c4a6e"
              stroke="#38bdf8"
            />
            <text
              x={chart.toX(6.79) - 29}
              y={chart.H - chart.padB - 18}
              textAnchor="middle"
              fill="#bae6fd"
              fontSize="10"
              fontWeight="bold"
              fontFamily="monospace"
            >
              6.79 dB
            </text>

            {energyMode === 'useful' ? (
              <>
                <line
                  x1={chart.toX(8.16)}
                  y1={chart.padT}
                  x2={chart.toX(8.16)}
                  y2={chart.H - chart.padB}
                  stroke="#f59e0b"
                  strokeDasharray="3 3"
                  strokeWidth={1.3}
                />
                <rect
                  x={chart.toX(8.16) + 4}
                  y={chart.H - chart.padB - 32}
                  width={50}
                  height={20}
                  rx={4}
                  fill="#78350f"
                  stroke="#f59e0b"
                />
                <text
                  x={chart.toX(8.16) + 29}
                  y={chart.H - chart.padB - 18}
                  textAnchor="middle"
                  fill="#fde68a"
                  fontSize="10"
                  fontWeight="bold"
                  fontFamily="monospace"
                >
                  8.16 dB
                </text>
              </>
            ) : (
              <>
                <line
                  x1={chart.toX(3.39)}
                  y1={chart.padT}
                  x2={chart.toX(3.39)}
                  y2={chart.H - chart.padB}
                  stroke="#f59e0b"
                  strokeDasharray="3 3"
                  strokeWidth={1.3}
                />
                <rect
                  x={chart.toX(3.39) - 54}
                  y={chart.H - chart.padB - 32}
                  width={50}
                  height={20}
                  rx={4}
                  fill="#78350f"
                  stroke="#f59e0b"
                />
                <text
                  x={chart.toX(3.39) - 29}
                  y={chart.H - chart.padB - 18}
                  textAnchor="middle"
                  fill="#fde68a"
                  fontSize="10"
                  fontWeight="bold"
                  fontFamily="monospace"
                >
                  3.39 dB
                </text>
              </>
            )}

            {/* Courbes théoriques */}
            <path d={chart.theoNrzPath} fill="none" stroke="#38bdf8" strokeWidth={2.2} />
            <path d={chart.theoUnipolPath} fill="none" stroke="#10b981" strokeWidth={2} />
            <path d={chart.theoNrztPath} fill="none" stroke="#a855f7" strokeWidth={2} />
            <path d={chart.theoCodeurPath} fill="none" stroke="#f59e0b" strokeWidth={2.2} />

            {/* Points simulés Java de référence (100 000 bits, graine 42) */}
            {snrGrid.map((snr, idx) => {
              const yNrz = ENV1_REFERENCE_DATA.curves['NRZ antipolaire [-1, 1]'][idx];
              const yRz = ENV1_REFERENCE_DATA.curves['RZ [0, 1]'][idx];
              const yNrzt = ENV1_REFERENCE_DATA.curves['NRZT antipolaire [-1, 1]'][idx];
              const yCod = ENV1_REFERENCE_DATA.curves['NRZ + Codeur (Eb/N0 utile)'][idx];
              return (
                <g key={`pts-${snr}`}>
                  {yNrz > 0 && (
                    <circle
                      cx={chart.toX(snr)}
                      cy={chart.toY(yNrz)}
                      r={4}
                      fill="#38bdf8"
                      stroke="#090d16"
                      strokeWidth={1.2}
                    />
                  )}
                  {yRz > 0 && (
                    <rect
                      x={chart.toX(snr) - 3.5}
                      y={chart.toY(yRz) - 3.5}
                      width={7}
                      height={7}
                      fill="#10b981"
                      stroke="#090d16"
                      strokeWidth={1}
                    />
                  )}
                  {yNrzt > 0 && (
                    <circle
                      cx={chart.toX(snr)}
                      cy={chart.toY(yNrzt)}
                      r={3.8}
                      fill="#a855f7"
                      stroke="#090d16"
                      strokeWidth={1}
                    />
                  )}
                  {energyMode === 'useful' && yCod > 0 && (
                    <rect
                      x={chart.toX(snr) - 4}
                      y={chart.toY(yCod) - 4}
                      width={8}
                      height={8}
                      fill="#f59e0b"
                      stroke="#090d16"
                      strokeWidth={1.2}
                    />
                  )}
                </g>
              );
            })}

            {/* Points Monte-Carlo live si lancés */}
            {liveResults &&
              snrGrid.map((snr, idx) => {
                const yCod = liveResults['NRZ + Codeur TP5'][idx];
                return (
                  <g key={`live-${snr}`}>
                    {yCod > 0 && (
                      <circle
                        cx={chart.toX(snr)}
                        cy={chart.toY(yCod)}
                        r={5}
                        fill="none"
                        stroke="#fde047"
                        strokeWidth={2}
                      />
                    )}
                  </g>
                );
              })}
          </svg>
        </div>

        {/* Légende */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-3 text-xs font-mono">
          <div className="flex items-center gap-2 text-sky-300">
            <span className="w-3 h-3 rounded-full bg-sky-400 inline-block" /> NRZ [-1, 1] (6.79 dB)
          </div>
          <div className="flex items-center gap-2 text-amber-300">
            <span className="w-3 h-3 rounded-sm bg-amber-400 inline-block" /> NRZ + Codeur 1/3 ({energyMode === 'useful' ? '8.16 dB' : '3.39 dB'})
          </div>
          <div className="flex items-center gap-2 text-emerald-300">
            <span className="w-3 h-3 rounded-sm bg-emerald-400 inline-block" /> RZ / NRZ [0, 1] (9.80 dB)
          </div>
          <div className="flex items-center gap-2 text-purple-300">
            <span className="w-3 h-3 rounded-full bg-purple-400 inline-block" /> NRZT [-1, 1] (10.47 dB)
          </div>
        </div>
      </div>
    </div>
  );
};
