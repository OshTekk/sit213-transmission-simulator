import React, { useState, useMemo } from 'react';
import { SimulationConfig, WaveformType, runSimulation } from '../engine/dsp';
import { Grid, ArrowUpRight, Layers } from 'lucide-react';

interface MultipathHeatmapViewProps {
  onLoadIntoOscilloscope: (patch: Partial<SimulationConfig>) => void;
}

export const MultipathHeatmapView: React.FC<MultipathHeatmapViewProps> = ({
  onLoadIntoOscilloscope,
}) => {
  const [form, setForm] = useState<WaveformType>('NRZ');
  const [unipolar, setUnipolar] = useState(false);
  const [snrDb, setSnrDb] = useState(10.0);
  const [numEchoes, setNumEchoes] = useState(1);
  const [selectedCell, setSelectedCell] = useState<{
    dt: number;
    alpha: number;
    teb: number;
  } | null>(null);

  const nbEch = 30;
  // Grille : 21 retards de 0 à 60 éch (pas de 3) x 11 amplitudes de 0.0 à 1.0 (pas de 0.1)
  const retards = useMemo(
    () => Array.from({ length: 21 }, (_, i) => i * 3),
    []
  );
  const alphas = useMemo(
    () => Array.from({ length: 11 }, (_, i) => Number((1.0 - i * 0.1).toFixed(1))),
    []
  );

  const heatmapData = useMemo(() => {
    const amplMin = form === 'RZ' || unipolar ? 0.0 : -1.0;
    const amplMax = 1.0;
    const grid: { dt: number; alpha: number; teb: number }[][] = [];
    let maxTeb = 0.01;

    for (const alpha of alphas) {
      const row: { dt: number; alpha: number; teb: number }[] = [];
      for (const dt of retards) {
        const echoes = Array.from({ length: numEchoes }, (_, k) => {
          const delayK = dt + k * 10;
          const arK =
            alpha * Math.exp(-(delayK - dt) / nbEch);
          return {
            id: `e-${k}`,
            dt: delayK,
            ar: Number(Math.min(1, Math.max(0, arK)).toFixed(3)),
          };
        });

        const sim = runSimulation({
          messageMode: 'random',
          nbBits: 450,
          fixedMessage: '10110010',
          useSeed: true,
          seed: 12345,
          analogMode: true,
          form,
          nbEch,
          amplMin,
          amplMax,
          useSnr: true,
          snrpbDb: snrDb,
          echoes,
          useCodeur: false,
        });

        if (sim.usefulTeb > maxTeb) maxTeb = sim.usefulTeb;
        row.push({ dt, alpha, teb: sim.usefulTeb });
      }
      grid.push(row);
    }

    return { grid, maxTeb, amplMin, amplMax };
  }, [form, unipolar, snrDb, numEchoes, alphas, retards]);

  // Palette Viridis-like fidèle aux cartes TP4 du rapport (Figures 26 à 31)
  const tebToColor = (teb: number, maxVal: number) => {
    const t = Math.min(1, Math.max(0, teb / Math.max(0.05, maxVal)));
    if (t < 0.25) {
      const u = t / 0.25;
      return `rgb(${Math.round(68 - 10 * u)}, ${Math.round(1 + 82 * u)}, ${Math.round(84 + 55 * u)})`;
    }
    if (t < 0.5) {
      const u = (t - 0.25) / 0.25;
      return `rgb(${Math.round(58 - 25 * u)}, ${Math.round(83 + 62 * u)}, ${Math.round(139 + 1 * u)})`;
    }
    if (t < 0.75) {
      const u = (t - 0.5) / 0.25;
      return `rgb(${Math.round(33 + 62 * u)}, ${Math.round(145 + 56 * u)}, ${Math.round(140 - 44 * u)})`;
    }
    const u = (t - 0.75) / 0.25;
    return `rgb(${Math.round(95 + 158 * u)}, ${Math.round(201 + 30 * u)}, ${Math.round(96 - 59 * u)})`;
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
      {/* CONTRÔLES TP4 */}
      <div className="lg:col-span-4 space-y-4">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg space-y-4">
          <h3 className="text-sm font-bold text-cyan-400 flex items-center gap-2">
            <Grid className="w-4 h-4" /> TP4 · Cartes de Chaleur Multitrajets
          </h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Reproduit en direct les scripts <code className="text-cyan-300">teb_multitrajet.py</code> et{' '}
            <code className="text-cyan-300">teb_multitrajets.py</code> (Figures 26 à 31 du rapport). Cliquez sur une cellule (dt, α) pour l'inspecter ou la charger dans l'oscilloscope.
          </p>

          <div>
            <label className="block text-xs text-slate-400 mb-1.5">
              Forme d'onde & Polarité
            </label>
            <div className="grid grid-cols-2 gap-2">
              {[
                { label: 'NRZ [-1, +1]', f: 'NRZ' as WaveformType, uni: false },
                { label: 'NRZ [0, +1]', f: 'NRZ' as WaveformType, uni: true },
                { label: 'RZ [0, +1]', f: 'RZ' as WaveformType, uni: true },
                { label: 'NRZT [-1, +1]', f: 'NRZT' as WaveformType, uni: false },
              ].map((opt) => {
                const active = form === opt.f && unipolar === opt.uni;
                return (
                  <button
                    key={opt.label}
                    onClick={() => {
                      setForm(opt.f);
                      setUnipolar(opt.uni);
                    }}
                    className={`py-2 px-2.5 rounded-lg text-xs font-mono font-semibold border transition ${
                      active
                        ? 'bg-cyan-500/20 border-cyan-400 text-cyan-200'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {opt.label}
                  </button>
                );
              })}
            </div>
          </div>

          <div>
            <div className="flex justify-between text-xs mb-1">
              <span className="text-slate-300">Rapport Eb/N0 (-snrpb)</span>
              <span className="font-mono font-bold text-amber-300">
                {snrDb.toFixed(1)} dB
              </span>
            </div>
            <input
              type="range"
              min={4}
              max={15}
              step={1}
              value={snrDb}
              onChange={(e) => setSnrDb(Number(e.target.value))}
              className="w-full accent-amber-400"
            />
          </div>

          <div>
            <div className="flex justify-between text-xs mb-1.5">
              <span className="text-slate-300 flex items-center gap-1.5">
                <Layers className="w-3.5 h-3.5 text-emerald-400" /> Nombre de trajets indirects (M)
              </span>
              <span className="font-mono font-bold text-emerald-300">
                {numEchoes} écho{numEchoes > 1 ? 's' : ''}
              </span>
            </div>
            <div className="grid grid-cols-5 gap-1.5">
              {[1, 2, 3, 4, 5].map((m) => (
                <button
                  key={m}
                  onClick={() => setNumEchoes(m)}
                  className={`py-1.5 rounded font-mono text-xs font-bold border transition ${
                    numEchoes === m
                      ? 'bg-emerald-500/20 border-emerald-400 text-emerald-200'
                      : 'bg-slate-950 border-slate-800 text-slate-400'
                  }`}
                >
                  {m}
                </button>
              ))}
            </div>
          </div>

          {/* Cellule sélectionnée */}
          {selectedCell && (
            <div className="p-3.5 rounded-xl bg-slate-950 border border-cyan-500/40 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-cyan-300">
                  Point sélectionné : dt = {selectedCell.dt} éch ({(selectedCell.dt / nbEch).toFixed(2)} T)
                </span>
                <span className="font-mono text-xs text-amber-300">
                  α = {selectedCell.alpha.toFixed(1)}
                </span>
              </div>
              <div className="text-sm font-mono font-bold text-slate-100">
                TEB mesuré = {selectedCell.teb.toFixed(4)}
              </div>
              <button
                onClick={() =>
                  onLoadIntoOscilloscope({
                    analogMode: true,
                    form,
                    nbEch: 30,
                    amplMin: heatmapData.amplMin,
                    amplMax: heatmapData.amplMax,
                    useSnr: true,
                    snrpbDb: snrDb,
                    echoes: Array.from({ length: numEchoes }, (_, k) => ({
                      id: `hm-${k}`,
                      dt: selectedCell.dt + k * 10,
                      ar: Number(
                        (
                          selectedCell.alpha *
                          Math.exp(-(k * 10) / 30)
                        ).toFixed(2)
                      ),
                    })),
                  })
                }
                className="w-full py-2 px-3 rounded-lg bg-cyan-500/20 hover:bg-cyan-500/30 border border-cyan-500/50 text-cyan-200 text-xs font-semibold flex items-center justify-center gap-1.5 transition"
              >
                <ArrowUpRight className="w-3.5 h-3.5" /> Inspecter ce signal dans l'Oscilloscope
              </button>
            </div>
          )}
        </div>
      </div>

      {/* CARTE 2D INTERACTIVE */}
      <div className="lg:col-span-8 bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg flex flex-col justify-between">
        <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
          <div>
            <h3 className="text-base font-bold text-slate-100">
              Carte TEB(dt, α) — {form} [{heatmapData.amplMin}, +{heatmapData.amplMax}] · {numEchoes} trajet{numEchoes > 1 ? 's' : ''} indirect{numEchoes > 1 ? 's' : ''} · SNR = {snrDb} dB
            </h3>
            <p className="text-xs text-slate-400">
              Abscisse : retard dt de 0 à 60 éch (0 à 2T) · Ordonnée : amplitude relative α de 0.0 à 1.0
            </p>
          </div>
          <div className="flex items-center gap-2 text-xs font-mono">
            <span className="text-slate-400">0.00</span>
            <div
              className="w-28 h-3 rounded"
              style={{
                background:
                  'linear-gradient(90deg, rgb(68,1,84), rgb(49,104,142), rgb(53,183,121), rgb(253,231,37))',
              }}
            />
            <span className="text-amber-300 font-bold">
              {heatmapData.maxTeb.toFixed(3)}
            </span>
          </div>
        </div>

        <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 overflow-x-auto">
          <div className="min-w-[540px]">
            {heatmapData.grid.map((row, rIdx) => (
              <div key={rIdx} className="flex items-center gap-1 mb-1">
                <div className="w-12 text-right pr-2 font-mono text-[11px] text-slate-400">
                  α={alphas[rIdx].toFixed(1)}
                </div>
                <div className="flex-1 grid grid-cols-21 gap-1">
                  {row.map((cell) => {
                    const isSelected =
                      selectedCell?.dt === cell.dt &&
                      selectedCell?.alpha === cell.alpha;
                    return (
                      <button
                        key={`${cell.dt}-${cell.alpha}`}
                        onClick={() => setSelectedCell(cell)}
                        title={`dt=${cell.dt} (${(cell.dt / 30).toFixed(2)}T), α=${cell.alpha} -> TEB=${cell.teb.toFixed(4)}`}
                        style={{
                          backgroundColor: tebToColor(
                            cell.teb,
                            heatmapData.maxTeb
                          ),
                        }}
                        className={`h-7 rounded-sm transition transform hover:scale-110 ${
                          isSelected ? 'ring-2 ring-white z-10' : ''
                        }`}
                      />
                    );
                  })}
                </div>
              </div>
            ))}

            {/* Axe X des retards */}
            <div className="flex items-center gap-1 mt-2 pt-1 border-t border-slate-800">
              <div className="w-12 text-right pr-2 font-mono text-[10px] text-slate-500">
                dt
              </div>
              <div className="flex-1 grid grid-cols-21 gap-1 text-center font-mono text-[10px] text-slate-400">
                {retards.map((dt) => (
                  <div key={dt}>
                    <div>{dt}</div>
                    {dt % 15 === 0 && (
                      <div className="text-[9px] text-cyan-400">
                        {dt === 0
                          ? '0'
                          : dt === 15
                          ? 'T/2'
                          : dt === 30
                          ? 'T'
                          : dt === 45
                          ? '3T/2'
                          : '2T'}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Analyse physique automatique */}
        <div className="mt-3 p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-300 leading-relaxed">
          {form === 'RZ' ? (
            <>
              <strong className="text-cyan-300">Analyse physique RZ (Figure 27) :</strong>{' '}
              Comme l'impulsion RZ n'occupe que le tiers central [T/3, 2T/3], l'écho ne perturbe la fenêtre de décision que lorsque le retard dt est proche d'un multiple entier de la période symbole (dt ≈ 30 = T ou dt ≈ 60 = 2T). Entre deux périodes (ex: dt = 15 = T/2), l'écho tombe dans les zones mortes latérales et le TEB reste très bas !
            </>
          ) : form === 'NRZT' ? (
            <>
              <strong className="text-purple-300">Analyse physique NRZT (Figure 28) :</strong>{' '}
              Les transitions obliques étalent l'interférence sur une plage plus large autour de dt = T (30 éch.), et l'intégration restreinte au tiers central accroît la sensibilité au bruit gaussien.
            </>
          ) : unipolar ? (
            <>
              <strong className="text-amber-300">Analyse physique NRZ Unipolaire [0, 1] :</strong>{' '}
              Dès que α ≥ 0.5 et dt ≥ 20 (2T/3), un écho de bit '1' décale le niveau d'un bit '0' au-delà du seuil θ = 0.5, provoquant une fermeture de l'œil et un TEB massif (&gt; 10 %).
            </>
          ) : (
            <>
              <strong className="text-emerald-300">Analyse physique NRZ Antipolaire [-1, +1] (Figure 26) :</strong>{' '}
              Tant que α ≤ 0.5, la marge d'ouverture de l'œil reste positive (1 - α ≥ 0.5 &gt; 0) sur toute la largeur des retards. La dégradation maximale n'apparaît que pour α → 1.0 à dt = T (30 éch.), où un écho opposé annule le symbole direct.
            </>
          )}
        </div>
      </div>
    </div>
  );
};
