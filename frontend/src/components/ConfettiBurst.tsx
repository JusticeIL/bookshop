import { useEffect, useRef } from 'react';

/** Rainbow palette for the celebration burst. */
const COLORS = ['#ef4444', '#f97316', '#f59e0b', '#facc15', '#10b981', '#38bdf8', '#6366f1', '#a855f7', '#ec4899'];

interface Piece {
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  color: string;
  rotation: number;
  spin: number;
}

/**
 * Dependency-free canvas confetti: two bottom-corner "party cannons" fire
 * rainbow pieces that tumble down under gravity, then the layer removes
 * itself. Rendered once when the order confirmation appears.
 */
export default function ConfettiBurst({ duration = 4000 }: { duration?: number }) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return;

    const resize = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    resize();
    window.addEventListener('resize', resize);

    const cannon = (originX: number, direction: number): Piece[] =>
      Array.from({ length: 90 }, () => ({
        x: originX,
        y: canvas.height * 0.9,
        vx: direction * (2 + Math.random() * 6),
        vy: -(9 + Math.random() * 8),
        size: 5 + Math.random() * 6,
        color: COLORS[Math.floor(Math.random() * COLORS.length)],
        rotation: Math.random() * Math.PI * 2,
        spin: (Math.random() - 0.5) * 0.35,
      }));

    const pieces: Piece[] = [
      ...cannon(canvas.width * 0.08, +1),
      ...cannon(canvas.width * 0.92, -1),
    ];

    let raf = 0;
    const start = performance.now();

    const tick = (now: number) => {
      const elapsed = now - start;
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const fade = Math.max(0, 1 - elapsed / duration);

      pieces.forEach((piece) => {
        piece.vy += 0.22; // gravity
        piece.vx *= 0.99; // drag
        piece.x += piece.vx;
        piece.y += piece.vy;
        piece.rotation += piece.spin;

        ctx.save();
        ctx.globalAlpha = fade;
        ctx.translate(piece.x, piece.y);
        ctx.rotate(piece.rotation);
        ctx.fillStyle = piece.color;
        ctx.fillRect(-piece.size / 2, -piece.size / 4, piece.size, piece.size / 2);
        ctx.restore();
      });

      if (elapsed < duration) {
        raf = requestAnimationFrame(tick);
      } else {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }
    };
    raf = requestAnimationFrame(tick);

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', resize);
    };
  }, [duration]);

  return <canvas ref={canvasRef} className="confetti-canvas" aria-hidden="true" />;
}
