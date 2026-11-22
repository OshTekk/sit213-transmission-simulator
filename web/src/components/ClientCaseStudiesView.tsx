import React, { useState, useMemo } from 'react';
import {
  SimulationConfig,
  ENV2_REFERENCE_DATA,
  dichotomie,
  tebTheoNrzAntipolaire,
  tebTheoAvecCodeurUtile,
  linToDb,
  dbToLin,
  Q,
} from '../engine/dsp';
import {
  BatteryCharging,
  Radio,
  ArrowUpRight,
  CheckCircle2,
  XCircle,
  Zap,
  Gauge,
} from 'lucide-react';

interface ClientCaseStudiesViewProps {
  onLoadIntoOscilloscope: (patch: Partial<SimulationConfig>) => void;
}

export const ClientCaseStudiesView: React.FC<ClientCaseStudiesViewProps> = ({
  onLoadIntoOscilloscope,
}) => {
  const [activeEnv, setActiveEnv] = useState<'env1' | 'env2'>('env1');

  // --- ÉTATS INTERACTIFS ENVIRONNEMENT 1 ---
  const [n0DbmHz, setN0DbmHz] = useState(-80.0);
  const [attenDb, setAttenDb] = useState(40.0);
  const [targetTebExp, setTargetTebExp] = useState(-3); // 10^-3
  const [batteryJoules, setBatteryJoules] = useState(3.0);
  const [bitsPerDay, setBitsPerDay] = useState(1e6);

  // Calculs analytiques exacts Environnement 1
  const env1Calc = useMemo(() => {
    const tebCible = Math.pow(10, targetTebExp);
    const n0WattsHz = Math.pow(10, (n0DbmHz - 30) / 10); // 1e-11 J pour -80 dBm/Hz
    const attenLin = Math.pow(10, attenDb / 10); // 10 000 pour 40 dB

    const ebn0SansLin = dichotomie(tebTheoNrzAntipolaire, tebCible, 0.01, 100);
    const ebn0AvecLin = dichotomie(tebTheoAvecCodeurUtile, tebCible, 0.01, 100);
    const ecN0CanalLin = ebn0AvecLin / 3.0;
    const pCanal = tebTheoNrzAntipolaire(ecN0CanalLin);

    // Sans codeur (seuil théorique)
    const ebRecuSans = ebn0SansLin * n0WattsHz;
    const ebEmisSans = ebRecuSans * attenLin;
    const consoJourSans = ebEmisSans * bitsPerDay;
    const joursSans = batteryJoules / consoJourSans;

    // Sans codeur (variante avec marge +0.21 dB -> 7.00 dB)
    const ebn0MargeDb = 7.0;
    const ebn0MargeLin = dbToLin(ebn0MargeDb);
    const ebEmisMarge = ebn0MargeLin * n0WattsHz * attenLin;
    const consoJourMarge = ebEmisMarge * bitsPerDay;
    const joursMarge = batteryJoules / consoJourMarge;
    const tebMarge = tebTheoNrzAntipolaire(ebn0MargeLin);

    // Avec codeur TP5 (3 bits canal par bit utile)
    const ebRecuAvec = ebn0AvecLin * n0WattsHz;
    const ebEmisAvec = ebRecuAvec * attenLin;
    const consoJourAvec = ebEmisAvec * bitsPerDay;
    const joursAvec = batteryJoules / consoJourAvec;

    const surconsoPct = (ebEmisAvec / ebEmisSans - 1) * 100;
    const perteAutonomiePct = (1 - joursAvec / joursSans) * 100;

    return {
      tebCible,
      n0WattsHz,
      attenLin,
      ebn0SansLin,
      ebn0SansDb: linToDb(ebn0SansLin),
      ebRecuSans,
      ebEmisSans,
      consoJourSans,
      joursSans,
      ebn0MargeDb,
      ebEmisMarge,
      joursMarge,
      tebMarge,
      ebn0AvecLin,
      ebn0AvecDb: linToDb(ebn0AvecLin),
      ecN0CanalLin,
      ecN0CanalDb: linToDb(ecN0CanalLin),
      pCanal,
      ebRecuAvec,
      ebEmisAvec,
      consoJourAvec,
      joursAvec,
      surconsoPct,
      perteAutonomiePct,
    };
  }, [n0DbmHz, attenDb, targetTebExp, batteryJoules, bitsPerDay]);

  // --- ÉTATS INTERACTIFS ENVIRONNEMENT 2 ---
  const [tauUs, setTauUs] = useState(10.0);
  const [alphaEcho, setAlphaEcho] = useState(0.5);
  const [debitKbps, setDebitKbps] = useState(100);
  const [env2SnrDb, setEnv2SnrDb] = useState(15.0);
  const [env2NbEch, setEnv2NbEch] = useState(30);

  const env2Calc = useMemo(() => {
    const dtSamples = Math.round(tauUs * 1e-6 * (debitKbps * 1e3) * env2NbEch);
    const tsUs = 1000 / debitKbps;
    const ratioTauTs = tauUs / tsUs;

    const snrLin = dbToLin(env2SnrDb);
    const sigmaNrz = Math.sqrt(1.0 / (2.0 * snrLin));
    const psNrzt = 0.778333;
    const sigmaNrzt = Math.sqrt((3.0 * psNrzt) / (2.0 * snrLin));

    const margeAntipol = Math.max(0, 1.0 - alphaEcho);
    const xNrz = margeAntipol / sigmaNrz;
    const xNrzt = margeAntipol / sigmaNrzt;

    const tebPireNrz = 0.5 * Q(xNrz);
    const tebPireNrzt = 0.5 * Q(xNrzt);

    // Niveaux reçus (4 combinaisons bit direct / bit retardé)
    const levelsAntipol = [
      { label: 'Bit 1 + Écho 1', val: 1 + alphaEcho, col: 'bg-emerald-500' },
      { label: 'Bit 1 + Écho 0 (Pire 1)', val: 1 - alphaEcho, col: 'bg-sky-500' },
      { label: 'Bit 0 + Écho 1 (Pire 0)', val: -1 + alphaEcho, col: 'bg-amber-500' },
      { label: 'Bit 0 + Écho 0', val: -1 - alphaEcho, col: 'bg-indigo-500' },
    ];

    const levelsUnipol = [
      { label: 'Bit 1 + Écho 1', val: 1 + alphaEcho, col: 'bg-emerald-500' },
      { label: 'Bit 1 + Écho 0 (Pire 1)', val: 1.0, col: 'bg-sky-500' },
      { label: 'Bit 0 + Écho 1 (Pire 0)', val: alphaEcho, col: 'bg-rose-500' },
      { label: 'Bit 0 + Écho 0', val: 0.0, col: 'bg-indigo-500' },
    ];

    return {
      dtSamples,
      tsUs,
      ratioTauTs,
      sigmaNrz,
      sigmaNrzt,
      margeAntipol,
      xNrz,
      xNrzt,
      tebPireNrz,
      tebPireNrzt,
      levelsAntipol,
      levelsUnipol,
    };
  }, [tauUs, alphaEcho, debitKbps, env2SnrDb, env2NbEch]);

  return (
    <div className="space-y-6">
      {/* SÉLECTEUR D'ENVIRONNEMENT */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <button
          onClick={() => setActiveEnv('env1')}
          className={`p-4 rounded-xl border text-left transition flex items-start justify-between ${
            activeEnv === 'env1'
              ? 'bg-cyan-950/50 border-cyan-400 shadow-lg'
              : 'bg-slate-900/70 border-slate-800 hover:border-slate-700'
          }`}
        >
          <div>
            <div className="flex items-center gap-2 text-sm font-bold text-cyan-300">
              <BatteryCharging className="w-4 h-4" /> Environnement 1 · Capteurs sur Batterie (Canal BBAG)
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Questions 1, 2 & 3 : Minimisation énergétique à TEB &le; 10⁻³, autonomie pour 3 Joules (10⁶ bits/jour) et bilan du codeur correcteur TP5.
            </p>
          </div>
          <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-cyan-500/20 text-cyan-200 border border-cyan-500/30">
            Q1 · Q2 · Q3
          </span>
        </button>

        <button
          onClick={() => setActiveEnv('env2')}
          className={`p-4 rounded-xl border text-left transition flex items-start justify-between ${
            activeEnv === 'env2'
              ? 'bg-emerald-950/50 border-emerald-400 shadow-lg'
              : 'bg-slate-900/70 border-slate-800 hover:border-slate-700'
          }`}
        >
          <div>
            <div className="flex items-center gap-2 text-sm font-bold text-emerald-300">
              <Gauge className="w-4 h-4" /> Environnement 2 · Émetteur Haut Débit (Canal à 2 Trajets)
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Questions 4 & 5 : Débit maximal pour τ = 10 µs et α₁ = 0.5 (TEB &le; 10⁻² à Eb/N0 &le; 15 dB), ouverture de l'œil NRZ vs NRZT et conditions pour 10⁻³.
            </p>
          </div>
          <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/20 text-emerald-200 border border-emerald-500/30">
            Q4 · Q5
          </span>
        </button>
      </div>

      {activeEnv === 'env1' ? (
        /* ==============================================================
           ENVIRONNEMENT 1 : CAPTEURS SUR BATTERIE (Q1, Q2, Q3)
           ============================================================== */
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Paramètres du cahier des charges */}
          <div className="lg:col-span-4 space-y-4">
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-cyan-400">
                  Cahier des Charges Client (Env. 1)
                </h3>
                <button
                  onClick={() => {
                    setN0DbmHz(-80);
                    setAttenDb(40);
                    setTargetTebExp(-3);
                    setBatteryJoules(3.0);
                    setBitsPerDay(1e6);
                  }}
                  className="text-[11px] font-mono text-slate-400 hover:text-cyan-300 underline"
                >
                  Réinitialiser sujet
                </button>
              </div>

              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-slate-300">Plancher de bruit N₀</span>
                  <span className="font-mono font-bold text-cyan-300">
                    {n0DbmHz} dBm/Hz ({env1Calc.n0WattsHz.toExponential(0)} J)
                  </span>
                </div>
                <input
                  type="range"
                  min={-100}
                  max={-60}
                  step={5}
                  value={n0DbmHz}
                  onChange={(e) => setN0DbmHz(Number(e.target.value))}
                  className="w-full accent-cyan-400"
                />
              </div>

              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-slate-300">Atténuation de ligne A</span>
                  <span className="font-mono font-bold text-amber-300">
                    {attenDb} dB (×{env1Calc.attenLin.toLocaleString()})
                  </span>
                </div>
                <input
                  type="range"
                  min={20}
                  max={60}
                  step={5}
                  value={attenDb}
                  onChange={(e) => setAttenDb(Number(e.target.value))}
                  className="w-full accent-amber-400"
                />
              </div>

              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-slate-300">Capacité de la batterie</span>
                  <span className="font-mono font-bold text-emerald-300">
                    {batteryJoules.toFixed(1)} Joules
                  </span>
                </div>
                <input
                  type="range"
                  min={1}
                  max={10}
                  step={0.5}
                  value={batteryJoules}
                  onChange={(e) => setBatteryJoules(Number(e.target.value))}
                  className="w-full accent-emerald-400"
                />
              </div>

              {/* Commandes de démo jury en 1 clic */}
              <div className="pt-3 border-t border-slate-800 space-y-2">
                <div className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                  <Zap className="w-3.5 h-3.5 text-cyan-400" /> Démonstrations Orales Directes
                </div>
                {[
                  {
                    title: '1. NRZ antipolaire @ 6.8 dB (Solution Q1)',
                    sub: 'TEB ≈ 1.07×10⁻³ (Théorie : 9.88×10⁻⁴)',
                    patch: {
                      analogMode: true,
                      form: 'NRZ' as const,
                      amplMin: -1,
                      amplMax: 1,
                      useSnr: true,
                      snrpbDb: 6.8,
                      echoes: [],
                      useCodeur: false,
                    },
                  },
                  {
                    title: '2. RZ unipolaire @ 6.8 dB (+3.01 dB pénalité)',
                    sub: 'TEB ≈ 1.45×10⁻² (14× plus dégradé)',
                    patch: {
                      analogMode: true,
                      form: 'RZ' as const,
                      amplMin: 0,
                      amplMax: 1,
                      useSnr: true,
                      snrpbDb: 6.8,
                      echoes: [],
                      useCodeur: false,
                    },
                  },
                  {
                    title: '3. NRZT antipolaire @ 6.8 dB (+3.68 dB pénalité)',
                    sub: 'TEB ≈ 2.19×10⁻² (20× plus dégradé)',
                    patch: {
                      analogMode: true,
                      form: 'NRZT' as const,
                      amplMin: -1,
                      amplMax: 1,
                      useSnr: true,
                      snrpbDb: 6.8,
                      echoes: [],
                      useCodeur: false,
                    },
                  },
                  {
                    title: '4. NRZ + Codeur TP5 @ 3.4 dB canal (= 8.2 dB utile)',
                    sub: 'TEB ≈ 1.01×10⁻³ (+37 % énergie par bit utile)',
                    patch: {
                      analogMode: true,
                      form: 'NRZ' as const,
                      amplMin: -1,
                      amplMax: 1,
                      useSnr: true,
                      snrpbDb: 3.4,
                      echoes: [],
                      useCodeur: true,
                    },
                  },
                ].map((d) => (
                  <button
                    key={d.title}
                    onClick={() => onLoadIntoOscilloscope(d.patch)}
                    className="w-full text-left p-2.5 rounded-lg bg-slate-950 hover:bg-cyan-950/40 border border-slate-800 hover:border-cyan-500/40 transition flex items-center justify-between group"
                  >
                    <div>
                      <div className="text-xs font-semibold text-slate-200 group-hover:text-cyan-300">
                        {d.title}
                      </div>
                      <div className="text-[11px] font-mono text-slate-400">
                        {d.sub}
                      </div>
                    </div>
                    <ArrowUpRight className="w-4 h-4 text-slate-500 group-hover:text-cyan-300 shrink-0" />
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Synthèse chiffrée Q1, Q2, Q3 */}
          <div className="lg:col-span-8 space-y-4">
            {/* Cartes comparatives Q1 / Q2 / Q3 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Carte 1 : Sans codeur (Minimum théorique) */}
              <div className="bg-slate-900/90 border-2 border-cyan-500/60 rounded-xl p-4 shadow-lg space-y-2">
                <div className="flex items-center justify-between">
                  <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-cyan-500/20 text-cyan-300">
                    Q1 & Q2 · OPTIMAL THÉORIQUE
                  </span>
                  <CheckCircle2 className="w-4 h-4 text-cyan-400" />
                </div>
                <div className="text-sm font-bold text-slate-100">
                  NRZ Antipolaire [-1, +1] sans codeur
                </div>
                <div className="text-2xl font-mono font-bold text-cyan-300">
                  {env1Calc.joursSans.toFixed(2)} jours
                </div>
                <div className="text-xs font-mono text-slate-300 space-y-1 pt-1 border-t border-slate-800">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Eb/N0 requis :</span>
                    <span className="font-bold">{env1Calc.ebn0SansDb.toFixed(2)} dB ({env1Calc.ebn0SansLin.toFixed(4)})</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Eb reçu :</span>
                    <span>{(env1Calc.ebRecuSans * 1e11).toFixed(4)}×10⁻¹¹ J</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Eb émis :</span>
                    <span className="text-cyan-300 font-bold">
                      {(env1Calc.ebEmisSans * 1e6).toFixed(4)} µJ/bit
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Conso / jour :</span>
                    <span>{env1Calc.consoJourSans.toFixed(4)} J/j</span>
                  </div>
                </div>
              </div>

              {/* Carte 2 : Variante avec marge (7.00 dB) */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg space-y-2">
                <div className="flex items-center justify-between">
                  <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-emerald-500/20 text-emerald-300">
                    VARIANTE AVEC MARGE
                  </span>
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                </div>
                <div className="text-sm font-bold text-slate-100">
                  NRZ Antipolaire @ 7.00 dB (+0.21 dB)
                </div>
                <div className="text-2xl font-mono font-bold text-emerald-300">
                  {env1Calc.joursMarge.toFixed(2)} jours
                </div>
                <div className="text-xs font-mono text-slate-300 space-y-1 pt-1 border-t border-slate-800">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Eb/N0 retenu :</span>
                    <span className="font-bold">7.00 dB (5.0119)</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">TEB théorique :</span>
                    <span>{(env1Calc.tebMarge * 1e4).toFixed(2)}×10⁻⁴</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Eb émis :</span>
                    <span className="text-emerald-300 font-bold">
                      {(env1Calc.ebEmisMarge * 1e6).toFixed(4)} µJ/bit
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Conso / jour :</span>
                    <span>{(env1Calc.ebEmisMarge * bitsPerDay).toFixed(4)} J/j</span>
                  </div>
                </div>
              </div>

              {/* Carte 3 : Avec codeur TP5 (Q3) */}
              <div className="bg-slate-900/90 border border-amber-500/50 rounded-xl p-4 shadow-lg space-y-2">
                <div className="flex items-center justify-between">
                  <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-amber-500/20 text-amber-300">
                    Q3 · AVEC CODEUR TP5 (1/3)
                  </span>
                  <XCircle className="w-4 h-4 text-amber-400" />
                </div>
                <div className="text-sm font-bold text-slate-100">
                  NRZ + Codeur Triplets (010 / 101)
                </div>
                <div className="text-2xl font-mono font-bold text-amber-300">
                  {env1Calc.joursAvec.toFixed(2)} jours
                </div>
                <div className="text-xs font-mono text-slate-300 space-y-1 pt-1 border-t border-slate-800">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Ec/N0 canal :</span>
                    <span>{env1Calc.ecN0CanalDb.toFixed(2)} dB ({env1Calc.ecN0CanalLin.toFixed(4)})</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Eb/N0 bit utile :</span>
                    <span className="font-bold text-amber-300">
                      {env1Calc.ebn0AvecDb.toFixed(2)} dB (+1.37 dB)
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Eb émis utile :</span>
                    <span className="text-amber-300 font-bold">
                      {(env1Calc.ebEmisAvec * 1e6).toFixed(4)} µJ (+{env1Calc.surconsoPct.toFixed(1)}%)
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Perte autonomie :</span>
                    <span className="text-rose-400 font-bold">
                      -{env1Calc.perteAutonomiePct.toFixed(1)} % (-1.70 j)
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Graphique comparatif d'autonomie et démonstration mathématique */}
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg space-y-4">
              <h4 className="text-sm font-bold text-slate-100">
                Bilan Énergétique & Démonstration Mathématique Complète (Environnement 1)
              </h4>

              {/* Barres visuelles comparatives */}
              <div className="space-y-3 bg-slate-950 p-4 rounded-xl border border-slate-800">
                {[
                  {
                    label: 'Sans codeur (Minimum théorique 6.79 dB)',
                    days: env1Calc.joursSans,
                    energy: env1Calc.ebEmisSans * 1e6,
                    color: 'bg-cyan-500',
                  },
                  {
                    label: 'Sans codeur (Variante avec marge 7.00 dB)',
                    days: env1Calc.joursMarge,
                    energy: env1Calc.ebEmisMarge * 1e6,
                    color: 'bg-emerald-500',
                  },
                  {
                    label: 'Avec codeur TP5 (3.39 dB canal = 8.16 dB utile)',
                    days: env1Calc.joursAvec,
                    energy: env1Calc.ebEmisAvec * 1e6,
                    color: 'bg-amber-500',
                  },
                ].map((item) => {
                  const pct = Math.min(100, (item.days / 8) * 100);
                  return (
                    <div key={item.label} className="space-y-1">
                      <div className="flex justify-between text-xs font-mono">
                        <span className="text-slate-200">{item.label}</span>
                        <span className="font-bold text-slate-100">
                          {item.days.toFixed(2)} jours ({item.energy.toFixed(4)} µJ/bit utile)
                        </span>
                      </div>
                      <div className="w-full h-3 bg-slate-900 rounded-full overflow-hidden">
                        <div
                          className={`h-full ${item.color} rounded-full transition-all duration-300`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Explication physique Q1 / Q3 */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs text-slate-300 leading-relaxed">
                <div className="p-3.5 rounded-lg bg-slate-950 border border-slate-800 space-y-1.5">
                  <div className="font-bold text-cyan-300">
                    Pourquoi NRZ antipolaire domine RZ et NRZT (Q1) :
                  </div>
                  <p>
                    • <strong>NRZ [-1, 1]</strong> : intégration sur tout le bit (filtre adapté optimal) et distance maximale d = 2A sans composante continue.
                  </p>
                  <p>
                    • <strong>NRZ [0, 1] & RZ [0, 1]</strong> : CodeRZ moyenne uniquement le tiers central (P_s = A²/6 pour bits équiprobables), donnant TEB = Q(√(Eb/N0)) soit <strong>+3.01 dB</strong> de pénalité.
                  </p>
                  <p>
                    • <strong>NRZT [-1, 1]</strong> : CodeNRZT moyenne uniquement le tiers central alors que les rampes latérales sont émises (P_s ≈ 0.778 A²), soit <strong>+3.68 dB</strong> de pénalité.
                  </p>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-950 border border-slate-800 space-y-1.5">
                  <div className="font-bold text-amber-300">
                    Pourquoi le codeur TP5 réduit l'autonomie de 27 % (Q3) :
                  </div>
                  <p>
                    • Le code à répétition inversée (0&rarr;010, 1&rarr;101) corrige 1 erreur sur 3 : TEB_utile = 3p² - 2p³ = 10⁻³ &rArr; p ≈ {(env1Calc.pCanal * 100).toFixed(2)} %, soit Ec/N0 = 3.39 dB (gain apparent de +3.40 dB sur le canal).
                  </p>
                  <p>
                    • Mais transmettre 3 bits canal par bit utile coûte +10 log₁₀(3) = <strong>+4.77 dB</strong>. Le bilan net est négatif : 3.39 + 4.77 = <strong>8.16 dB</strong> (+1.37 dB, soit +37.05 % d'énergie consommée).
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* ==============================================================
           ENVIRONNEMENT 2 : HAUT DÉBIT & MULTITRAJETS (Q4 & Q5)
           ============================================================== */
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Panneau de gauche : Convertisseur physique & démos */}
          <div className="lg:col-span-4 space-y-4">
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-emerald-400 flex items-center gap-2">
                  <Radio className="w-4 h-4" /> Convertisseur Temps &harr; Débit (Env. 2)
                </h3>
                <button
                  onClick={() => {
                    setTauUs(10.0);
                    setAlphaEcho(0.5);
                    setDebitKbps(100);
                    setEnv2SnrDb(15.0);
                    setEnv2NbEch(30);
                  }}
                  className="text-[11px] font-mono text-slate-400 hover:text-emerald-300 underline"
                >
                  Réinitialiser Q4
                </button>
              </div>

              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-slate-300">Débit binaire D</span>
                  <span className="font-mono font-bold text-emerald-300">
                    {debitKbps} kbit/s (Ts = {env2Calc.tsUs.toFixed(1)} µs)
                  </span>
                </div>
                <input
                  type="range"
                  min={10}
                  max={500}
                  step={10}
                  value={debitKbps}
                  onChange={(e) => setDebitKbps(Number(e.target.value))}
                  className="w-full accent-emerald-400"
                />
              </div>

              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-slate-300">Amplitude relative écho α₁</span>
                  <span className="font-mono font-bold text-cyan-300">
                    α₁ = {alphaEcho.toFixed(2)} (Marge antipol: {env2Calc.margeAntipol.toFixed(2)})
                  </span>
                </div>
                <input
                  type="range"
                  min={0.1}
                  max={0.95}
                  step={0.05}
                  value={alphaEcho}
                  onChange={(e) => setAlphaEcho(Number(e.target.value))}
                  className="w-full accent-cyan-400"
                />
              </div>

              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-slate-300">Rapport Eb/N0 récepteur</span>
                  <span className="font-mono font-bold text-amber-300">
                    {env2SnrDb.toFixed(1)} dB (Max autorisé : 15 dB)
                  </span>
                </div>
                <input
                  type="range"
                  min={6}
                  max={15}
                  step={0.5}
                  value={env2SnrDb}
                  onChange={(e) => setEnv2SnrDb(Number(e.target.value))}
                  className="w-full accent-amber-400"
                />
              </div>

              {/* Encadré de conversion dt = tau * D * Ne */}
              <div className="p-3.5 rounded-xl bg-slate-950 border border-emerald-500/40 font-mono text-xs space-y-1.5">
                <div className="text-emerald-300 font-bold">
                  Formule : dt = τ × D × Nₑ
                </div>
                <div className="flex justify-between text-slate-300">
                  <span>Retard τ = {tauUs} µs, Nₑ = {env2NbEch} :</span>
                  <span className="font-bold text-cyan-300">
                    dt = {env2Calc.dtSamples} éch.
                  </span>
                </div>
                <div className="flex justify-between text-slate-400 text-[11px]">
                  <span>Recouvrement τ / Ts :</span>
                  <span>{env2Calc.ratioTauTs.toFixed(2)} période(s) symbole</span>
                </div>
                <div className="flex justify-between text-slate-400 text-[11px]">
                  <span>TEB analytique NRZ [-1,1] :</span>
                  <span className="text-emerald-300 font-bold">
                    {env2Calc.tebPireNrz.toExponential(2)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400 text-[11px]">
                  <span>TEB analytique NRZT [-1,1] :</span>
                  <span className="text-amber-300 font-bold">
                    {env2Calc.tebPireNrzt.toExponential(2)}
                  </span>
                </div>
              </div>

              {/* Boutons de test direct dans l'Oscilloscope */}
              <div className="pt-2 border-t border-slate-800 space-y-2">
                <div className="text-xs font-semibold text-slate-300">
                  Charger dans l'Oscilloscope :
                </div>
                {[
                  {
                    title: `1. NRZ [-1, 1] @ ${debitKbps} kbit/s (dt=${env2Calc.dtSamples})`,
                    sub: 'Œil ouvert (marge +0.5 > 0) · TEB ~ 3×10⁻⁵ << 10⁻²',
                    patch: {
                      analogMode: true,
                      form: 'NRZ' as const,
                      nbEch: env2NbEch,
                      amplMin: -1,
                      amplMax: 1,
                      useSnr: true,
                      snrpbDb: env2SnrDb,
                      echoes: [{ id: 'q4', dt: env2Calc.dtSamples, ar: alphaEcho }],
                      useCodeur: false,
                    },
                  },
                  {
                    title: `2. NRZT [-1, 1] @ ${debitKbps} kbit/s (dt=${env2Calc.dtSamples})`,
                    sub: 'Intégration tiers central (σ bruit ×√3) · TEB ~ 2.5×10⁻³',
                    patch: {
                      analogMode: true,
                      form: 'NRZT' as const,
                      nbEch: env2NbEch,
                      amplMin: -1,
                      amplMax: 1,
                      useSnr: true,
                      snrpbDb: env2SnrDb,
                      echoes: [{ id: 'q4', dt: env2Calc.dtSamples, ar: alphaEcho }],
                      useCodeur: false,
                    },
                  },
                  {
                    title: `3. NRZ Unipolaire [0, 1] @ ${debitKbps} kbit/s`,
                    sub: 'Contre-exemple : 0 + écho 0.5 = seuil 0.5 (Œil fermé !)',
                    patch: {
                      analogMode: true,
                      form: 'NRZ' as const,
                      nbEch: env2NbEch,
                      amplMin: 0,
                      amplMax: 1,
                      useSnr: true,
                      snrpbDb: env2SnrDb,
                      echoes: [{ id: 'q4', dt: env2Calc.dtSamples, ar: alphaEcho }],
                      useCodeur: false,
                    },
                  },
                ].map((b) => (
                  <button
                    key={b.title}
                    onClick={() => onLoadIntoOscilloscope(b.patch)}
                    className="w-full text-left p-2.5 rounded-lg bg-slate-950 hover:bg-emerald-950/40 border border-slate-800 hover:border-emerald-500/40 transition flex items-center justify-between group"
                  >
                    <div>
                      <div className="text-xs font-semibold text-slate-200 group-hover:text-emerald-300">
                        {b.title}
                      </div>
                      <div className="text-[11px] font-mono text-slate-400">
                        {b.sub}
                      </div>
                    </div>
                    <ArrowUpRight className="w-4 h-4 text-slate-500 group-hover:text-emerald-300 shrink-0" />
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Panneau de droite : Analyse de l'ouverture de l'œil + Courbes Q4 & Q5 */}
          <div className="lg:col-span-8 space-y-4">
            {/* Comparaison visuelle de l'ouverture de l'œil : Antipolaire vs Unipolaire */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-slate-900/90 border border-emerald-500/40 rounded-xl p-4 shadow-lg">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-emerald-300">
                    NRZ Antipolaire [-1, +1] · ŒIL OUVERT
                  </span>
                  <span className="text-[11px] font-mono text-slate-400">
                    Seuil θ = 0.0
                  </span>
                </div>
                <div className="space-y-2 bg-slate-950 p-3 rounded-lg border border-slate-800">
                  {env2Calc.levelsAntipol.map((lv) => (
                    <div key={lv.label} className="space-y-0.5">
                      <div className="flex justify-between text-[11px] font-mono">
                        <span className="text-slate-300">{lv.label}</span>
                        <span
                          className={
                            lv.val > 0 ? 'text-emerald-300 font-bold' : 'text-amber-300 font-bold'
                          }
                        >
                          {lv.val >= 0 ? `+${lv.val.toFixed(2)}` : lv.val.toFixed(2)}
                        </span>
                      </div>
                      <div className="w-full h-2 bg-slate-900 rounded overflow-hidden flex">
                        <div
                          className={`h-full ${lv.col}`}
                          style={{
                            width: `${Math.min(100, (Math.abs(lv.val) / 2) * 100)}%`,
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
                <p className="text-[11px] text-slate-300 mt-2">
                  Marge minimale au seuil 0 : <strong className="text-emerald-300">1.0 - {alphaEcho.toFixed(2)} = +{env2Calc.margeAntipol.toFixed(2)} &gt; 0</strong>. L'écho n'inverse jamais le signe du bit direct, quel que soit le débit D !
                </p>
              </div>

              <div className="bg-slate-900/90 border border-rose-500/40 rounded-xl p-4 shadow-lg">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-rose-300">
                    NRZ Unipolaire [0, +1] · ŒIL FERMÉ (si D &ge; 69 kbit/s)
                  </span>
                  <span className="text-[11px] font-mono text-slate-400">
                    Seuil θ = 0.5
                  </span>
                </div>
                <div className="space-y-2 bg-slate-950 p-3 rounded-lg border border-slate-800">
                  {env2Calc.levelsUnipol.map((lv) => (
                    <div key={lv.label} className="space-y-0.5">
                      <div className="flex justify-between text-[11px] font-mono">
                        <span className="text-slate-300">{lv.label}</span>
                        <span
                          className={
                            Math.abs(lv.val - 0.5) < 0.05
                              ? 'text-rose-400 font-bold'
                              : 'text-slate-200 font-bold'
                          }
                        >
                          +{lv.val.toFixed(2)}{' '}
                          {Math.abs(lv.val - 0.5) < 0.05 ? '(= SEUIL !)' : ''}
                        </span>
                      </div>
                      <div className="w-full h-2 bg-slate-900 rounded overflow-hidden">
                        <div
                          className={`h-full ${lv.col}`}
                          style={{
                            width: `${Math.min(100, (lv.val / 1.6) * 100)}%`,
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
                <p className="text-[11px] text-slate-300 mt-2">
                  Pour un bit '0' suivi d'un écho de bit '1', le niveau reçu vaut <strong className="text-rose-300">0 + 0.5 = 0.5 = θ</strong> : fermeture de l'œil (~12.6 % d'erreurs dès que l'écho recouvre le bit).
                </p>
              </div>
            </div>

            {/* Tableau récapitulatif des résultats Q4 & Q5 */}
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg space-y-4">
              <h4 className="text-sm font-bold text-slate-100">
                Synthèse Complète des Réponses aux Questions 4 & 5 (Antipolaire & Unipolaire)
              </h4>

              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs font-mono">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400">
                      <th className="py-2 pr-3">Configuration</th>
                      <th className="py-2 px-3">TEB @ 100 kbit/s (15 dB)</th>
                      <th className="py-2 px-3">Seuil SNR (TEB &le; 10⁻²)</th>
                      <th className="py-2 px-3">Débit max (TEB &le; 10⁻²)</th>
                      <th className="py-2 pl-3">Condition Q5 (TEB &le; 10⁻³)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/70">
                    <tr className="text-emerald-300 bg-emerald-950/20">
                      <td className="py-2.5 pr-3 font-bold">NRZ Antipolaire [-1, 1]</td>
                      <td className="py-2.5 px-3 font-bold">3.0 × 10⁻⁵</td>
                      <td className="py-2.5 px-3">9.3–9.4 dB (Marge 5.6 dB)</td>
                      <td className="py-2.5 px-3 font-bold">100 kbit/s (canonique) à &ge; 1 Mbit/s</td>
                      <td className="py-2.5 pl-3">Déjà &le; 10⁻³ dès Eb/N0 &ge; 11.4 dB</td>
                    </tr>
                    <tr className="text-amber-300">
                      <td className="py-2.5 pr-3 font-bold">NRZT Antipolaire [-1, 1]</td>
                      <td className="py-2.5 px-3">2.54 × 10⁻³ (~100× NRZ)</td>
                      <td className="py-2.5 px-3">12.9–13.1 dB (Marge 1.9 dB)</td>
                      <td className="py-2.5 px-3">Valide à 15 dB (palier ~2.3×10⁻³)</td>
                      <td className="py-2.5 pl-3">D &le; 84 kbit/s ou écho α₁ &le; 0.38</td>
                    </tr>
                    <tr className="text-sky-300">
                      <td className="py-2.5 pr-3">NRZ Unipolaire [0, 1]</td>
                      <td className="py-2.5 px-3 text-rose-400">1.26 × 10⁻¹ (Œil fermé)</td>
                      <td className="py-2.5 px-3">14.9 dB @ 69 kbit/s</td>
                      <td className="py-2.5 px-3 font-bold">≈ 69 kbit/s</td>
                      <td className="py-2.5 pl-3">D &le; 53 kbit/s (ou Codeur &rarr; 23 kbit/s utiles)</td>
                    </tr>
                    <tr className="text-purple-300">
                      <td className="py-2.5 pr-3">NRZT Unipolaire [0, 1]</td>
                      <td className="py-2.5 px-3 text-rose-400">&gt; 1.2 × 10⁻¹</td>
                      <td className="py-2.5 px-3">14.9 dB @ 49 kbit/s</td>
                      <td className="py-2.5 px-3">≈ 49 kbit/s</td>
                      <td className="py-2.5 pl-3">D &le; 32 kbit/s</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              {/* Mini-tableau des points mesurés Java (resultats_q4.json) */}
              <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                <div className="text-xs font-semibold text-slate-300">
                  Mesures de référence Java à Eb/N0 = 15 dB (100 000 bits, graine 42, Ne = 30) :
                </div>
                <div className="grid grid-cols-3 sm:grid-cols-6 gap-2 text-center font-mono text-xs">
                  {[10, 50, 75, 100, 200, 500].map((d) => {
                    const idx = ENV2_REFERENCE_DATA.debitsKbps.indexOf(d);
                    const vNrz =
                      ENV2_REFERENCE_DATA.debitCurves15dB['NRZ antipolaire [-1, 1]'][idx];
                    const vNrzt =
                      ENV2_REFERENCE_DATA.debitCurves15dB['NRZT antipolaire [-1, 1]'][idx];
                    return (
                      <div
                        key={d}
                        className="p-2 rounded bg-slate-900 border border-slate-800"
                      >
                        <div className="text-[11px] text-slate-400 font-bold">
                          {d} kbit/s
                        </div>
                        <div className="text-emerald-300 text-[11px] mt-0.5">
                          NRZ: {vNrz === 0 ? '0' : vNrz.toExponential(0)}
                        </div>
                        <div className="text-amber-300 text-[11px]">
                          NRZT: {vNrzt === 0 ? '0' : vNrzt.toExponential(1)}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
