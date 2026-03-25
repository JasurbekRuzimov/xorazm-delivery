import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

const style = document.createElement('style');
style.textContent = `
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  :root {
    --bg:        #0c0f0e;
    --bg2:       #131918;
    --bg3:       #1a2220;
    --bg4:       #222c2a;
    --border:    rgba(255,255,255,0.07);
    --border2:   rgba(255,255,255,0.12);
    --green:     #3ecf8e;
    --green2:    #2ab87a;
    --green-dim: rgba(62,207,142,0.12);
    --gold:      #f0b429;
    --gold-dim:  rgba(240,180,41,0.12);
    --red:       #e5534b;
    --red-dim:   rgba(229,83,75,0.12);
    --blue:      #4d9eff;
    --blue-dim:  rgba(77,158,255,0.12);
    --text:      #f0f5f2;
    --text2:     #8a9e98;
    --text3:     #566860;
    --radius:    12px;
    --radius-lg: 16px;
  }
  body {
    font-family: 'Outfit', 'DM Sans', -apple-system, sans-serif;
    background: var(--bg);
    color: var(--text);
    -webkit-font-smoothing: antialiased;
  }
  button, input, select, textarea { font-family: inherit; }
  a { color: inherit; text-decoration: none; }
  input, textarea, select {
    background: var(--bg3);
    border: 1px solid var(--border2);
    color: var(--text);
    border-radius: var(--radius);
    padding: 11px 14px;
    font-size: 14px;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
    width: 100%;
  }
  input::placeholder, textarea::placeholder { color: var(--text3); }
  input:focus, textarea:focus, select:focus {
    border-color: var(--green);
    box-shadow: 0 0 0 3px rgba(62,207,142,0.1);
  }
  ::-webkit-scrollbar { width: 6px; }
  ::-webkit-scrollbar-track { background: var(--bg2); }
  ::-webkit-scrollbar-thumb { background: var(--bg4); border-radius: 3px; }
`;
document.head.appendChild(style);

const link = document.createElement('link');
link.rel  = 'stylesheet';
link.href = 'https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&family=DM+Mono:wght@400;500&display=swap';
document.head.appendChild(link);

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode><App /></React.StrictMode>
);
