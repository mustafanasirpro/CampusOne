import type { ReactNode } from "react";

// Stylised illustrations for the "get your own API key" guide. These are simple
// mock-ups (not live screenshots) that show roughly what each screen looks like
// and mark exactly where to click with a numbered pin + arrow. Kept intentionally
// generic so they stay useful even as the real consoles get minor restyles.

const INK = "#1e293b";
const MUTE = "#94a3b8";
const LINE = "#e2e8f0";
const BRAND = "#4f46e5";
const BRAND_SOFT = "#eef2ff";

/** A little browser window frame with an address bar; scene content draws inside. */
function GuideShot({ url, children }: { url: string; children: ReactNode }) {
  return (
    <svg
      viewBox="0 0 520 300"
      className="h-auto w-full rounded-xl border border-slate-200 bg-white"
      role="img"
      aria-label={`Illustration of ${url}`}
    >
      {/* window chrome */}
      <rect x="0" y="0" width="520" height="44" fill="#f8fafc" />
      <line x1="0" y1="44" x2="520" y2="44" stroke={LINE} />
      <circle cx="20" cy="22" r="5" fill="#f87171" />
      <circle cx="38" cy="22" r="5" fill="#fbbf24" />
      <circle cx="56" cy="22" r="5" fill="#34d399" />
      <rect x="80" y="12" width="420" height="20" rx="10" fill="#fff" stroke={LINE} />
      <text x="94" y="26" fontSize="12" fill={MUTE} fontFamily="monospace">
        {url}
      </text>
      {children}
    </svg>
  );
}

/** A pulsing highlight ring around a click target, with a numbered pin + arrow. */
function Pin({
  x,
  y,
  w,
  h,
  n,
  from = "top",
}: {
  x: number;
  y: number;
  w: number;
  h: number;
  n: number;
  from?: "top" | "left" | "right";
}) {
  const badge =
    from === "left"
      ? { bx: x - 34, by: y + h / 2 }
      : from === "right"
        ? { bx: x + w + 34, by: y + h / 2 }
        : { bx: x + w / 2, by: y - 30 };
  return (
    <g>
      <rect
        x={x - 4}
        y={y - 4}
        width={w + 8}
        height={h + 8}
        rx={10}
        fill="none"
        stroke={BRAND}
        strokeWidth={2.5}
        strokeDasharray="6 5"
      >
        <animate
          attributeName="opacity"
          values="1;0.35;1"
          dur="1.4s"
          repeatCount="indefinite"
        />
      </rect>
      <line
        x1={badge.bx}
        y1={badge.by}
        x2={from === "left" ? x - 4 : from === "right" ? x + w + 4 : x + w / 2}
        y2={from === "top" ? y - 4 : y + h / 2}
        stroke={BRAND}
        strokeWidth={2}
      />
      <circle cx={badge.bx} cy={badge.by} r={13} fill={BRAND} />
      <text
        x={badge.bx}
        y={badge.by + 4}
        fontSize="13"
        fontWeight="700"
        fill="#fff"
        textAnchor="middle"
      >
        {n}
      </text>
    </g>
  );
}

function Btn({
  x,
  y,
  w,
  label,
  primary,
}: {
  x: number;
  y: number;
  w: number;
  label: string;
  primary?: boolean;
}) {
  return (
    <g>
      <rect
        x={x}
        y={y}
        width={w}
        height={30}
        rx={8}
        fill={primary ? BRAND : "#fff"}
        stroke={primary ? BRAND : LINE}
      />
      <text
        x={x + w / 2}
        y={y + 19}
        fontSize="12.5"
        fontWeight="600"
        fill={primary ? "#fff" : INK}
        textAnchor="middle"
      >
        {label}
      </text>
    </g>
  );
}

export function SignInScene({ n, brand }: { n: number; brand: string }) {
  return (
    <g>
      <text x="260" y="110" fontSize="17" fontWeight="700" fill={INK} textAnchor="middle">
        Sign in to {brand}
      </text>
      <text x="260" y="132" fontSize="12" fill={MUTE} textAnchor="middle">
        It&apos;s free — use Google, GitHub, or email
      </text>
      <g>
        <Btn x={160} y={160} w={200} label="Continue with Google" />
        <Pin x={160} y={160} w={200} h={30} n={n} from="right" />
      </g>
      <Btn x={160} y={202} w={200} label="Continue with GitHub" />
    </g>
  );
}

export function SidebarScene({
  n,
  item,
  url,
}: {
  n: number;
  item: string;
  url: string;
}) {
  const items = ["Playground", "Usage", item, "Settings"];
  return (
    <g>
      <rect x="0" y="45" width="150" height="255" fill="#fafafa" />
      <line x1="150" y1="45" x2="150" y2="300" stroke={LINE} />
      <text x="20" y="80" fontSize="13" fontWeight="700" fill={INK}>
        {url}
      </text>
      {items.map((label, i) => {
        const y = 108 + i * 34;
        const active = label === item;
        return (
          <g key={label}>
            {active ? (
              <rect x="12" y={y - 16} width="126" height="26" rx="7" fill={BRAND_SOFT} />
            ) : null}
            <text
              x="24"
              y={y}
              fontSize="12.5"
              fontWeight={active ? "700" : "500"}
              fill={active ? BRAND : MUTE}
            >
              {label}
            </text>
          </g>
        );
      })}
      <Pin x={12} y={108 + 2 * 34 - 16} w={126} h={26} n={n} from="right" />
      <rect x="178" y="80" width="300" height="24" rx="6" fill="#f1f5f9" />
      <rect x="178" y="120" width="300" height="60" rx="8" fill="#f8fafc" stroke={LINE} />
    </g>
  );
}

export function CreateKeyScene({
  n,
  label,
  title,
}: {
  n: number;
  label: string;
  title: string;
}) {
  return (
    <g>
      <text x="40" y="84" fontSize="16" fontWeight="700" fill={INK}>
        {title}
      </text>
      <text x="40" y="106" fontSize="12" fill={MUTE}>
        Your keys let apps use your account.
      </text>
      <g>
        <Btn x={330} y={70} w={150} label={label} primary />
        <Pin x={330} y={70} w={150} h={30} n={n} from="top" />
      </g>
      {/* an empty keys table */}
      <rect x="40" y="130" width="440" height="34" rx="8" fill="#f8fafc" stroke={LINE} />
      <text x="56" y="151" fontSize="11.5" fill={MUTE}>
        No keys yet — create one to get started
      </text>
    </g>
  );
}

export function NameKeyScene({ n, action }: { n: number; action: string }) {
  return (
    <g>
      <rect x="0" y="45" width="520" height="255" fill="rgba(15,23,42,0.08)" />
      <rect x="120" y="90" width="280" height="150" rx="14" fill="#fff" stroke={LINE} />
      <text x="140" y="120" fontSize="14" fontWeight="700" fill={INK}>
        Name your key
      </text>
      <rect x="140" y="134" width="240" height="30" rx="7" fill="#fff" stroke={LINE} />
      <text x="152" y="153" fontSize="12" fill={MUTE}>
        my-tutor-key
      </text>
      <g>
        <Btn x={280} y={190} w={100} label={action} primary />
        <Pin x={280} y={190} w={100} h={30} n={n} from="left" />
      </g>
    </g>
  );
}

export function CopyKeyScene({ n }: { n: number }) {
  return (
    <g>
      <rect x="0" y="45" width="520" height="255" fill="rgba(15,23,42,0.08)" />
      <rect x="90" y="86" width="340" height="160" rx="14" fill="#fff" stroke={LINE} />
      <text x="110" y="116" fontSize="14" fontWeight="700" fill={INK}>
        Save your key now
      </text>
      <text x="110" y="136" fontSize="11" fill="#dc2626">
        You won&apos;t be able to see it again!
      </text>
      <rect x="110" y="150" width="220" height="32" rx="7" fill="#0f172a" />
      <text x="124" y="171" fontSize="12" fill="#a5b4fc" fontFamily="monospace">
        ••••••••••••••••••••
      </text>
      <g>
        <Btn x={340} y={151} w={66} label="Copy" primary />
        <Pin x={340} y={151} w={66} h={30} n={n} from="top" />
      </g>
      <text x="110" y="214" fontSize="11.5" fill={MUTE}>
        Then come back here and paste it below.
      </text>
    </g>
  );
}

export { GuideShot };
