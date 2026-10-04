import { createContext, useCallback, useContext, useMemo, useState } from "react";

const TutorContext = createContext(null);

/** Estado del panel del Tutor IA: abierto/cerrado y la lección que se está viendo (RF-024). */
export function TutorProvider({ children }) {
  const [abierto, setAbierto] = useState(false);
  const [contexto, setContexto] = useState(null);

  const abrir = useCallback(() => setAbierto(true), []);
  const cerrar = useCallback(() => setAbierto(false), []);
  const alternar = useCallback(() => setAbierto((a) => !a), []);

  const valor = useMemo(() => ({ abierto, abrir, cerrar, alternar, contexto, setContexto }),
    [abierto, abrir, cerrar, alternar, contexto]);
  return <TutorContext.Provider value={valor}>{children}</TutorContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useTutor() {
  const ctx = useContext(TutorContext);
  if (!ctx) throw new Error("useTutor debe usarse dentro de TutorProvider");
  return ctx;
}
