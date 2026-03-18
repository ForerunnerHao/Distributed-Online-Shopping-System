/// <reference types="vite/client" />

// Optional: declare your env keys for better intellisense
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}


