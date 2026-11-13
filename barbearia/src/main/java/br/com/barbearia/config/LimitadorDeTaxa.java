package br.com.barbearia.config;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador por chave (ex.: IP) em janelas fixas de tempo, em memória.
 * Suficiente para uma única instância da aplicação.
 */
public class LimitadorDeTaxa {

    private record Janela(long inicioMs, int contador) {
    }

    private final int maximo;
    private final long janelaMs;
    private final Clock clock;
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();

    public LimitadorDeTaxa(int maximo, Duration janela, Clock clock) {
        this.maximo = maximo;
        this.janelaMs = janela.toMillis();
        this.clock = clock;
    }

    /** Registra uma ocorrência e diz se ainda está dentro do limite. */
    public boolean permitir(String chave) {
        return registrar(chave) <= maximo;
    }

    /** Registra uma ocorrência e devolve quantas houve na janela atual. */
    public int registrar(String chave) {
        long agora = clock.millis();
        limparSeNecessario(agora);
        return janelas.compute(chave, (k, j) -> j == null || agora - j.inicioMs() >= janelaMs
                ? new Janela(agora, 1)
                : new Janela(j.inicioMs(), j.contador() + 1)).contador();
    }

    /** Diz se a chave já atingiu o limite, sem registrar nada. */
    public boolean bloqueado(String chave) {
        Janela j = janelas.get(chave);
        return j != null && clock.millis() - j.inicioMs() < janelaMs && j.contador() >= maximo;
    }

    public void zerar(String chave) {
        janelas.remove(chave);
    }

    /** Segundos até a janela da chave terminar (para o cabeçalho Retry-After). */
    public long segundosParaLiberar(String chave) {
        Janela j = janelas.get(chave);
        if (j == null) {
            return 0;
        }
        return Math.max(1, (j.inicioMs() + janelaMs - clock.millis() + 999) / 1000);
    }

    private void limparSeNecessario(long agora) {
        if (janelas.size() > 10_000) {
            janelas.values().removeIf(j -> agora - j.inicioMs() >= janelaMs);
        }
    }
}
