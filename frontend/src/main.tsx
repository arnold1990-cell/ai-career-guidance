import React from "react";
import { createRoot } from "react-dom/client";

function App() {
  return (
    <main className="p-6">
      <h1>EduRite</h1>
      <p>Frontend foundation scaffolded with React + Vite + TypeScript.</p>
    </main>
  );
}

const container = document.getElementById("root");
if (container) {
  createRoot(container).render(<App />);
}
