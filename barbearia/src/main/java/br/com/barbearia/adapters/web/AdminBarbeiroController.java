package br.com.barbearia.adapters.web;

import br.com.barbearia.application.BarbeiroService;
import br.com.barbearia.domain.Barbeiro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/barbeiros")
class AdminBarbeiroController {

    record BarbeiroAdminDto(Long id, String nome, boolean ativo) {
        static BarbeiroAdminDto de(Barbeiro b) {
            return new BarbeiroAdminDto(b.getId(), b.getNome(), b.isAtivo());
        }
    }

    record NomeRequest(@NotBlank @Size(max = 60) String nome) {
    }

    private final BarbeiroService service;

    AdminBarbeiroController(BarbeiroService service) {
        this.service = service;
    }

    @GetMapping
    List<BarbeiroAdminDto> listar() {
        return service.listarTodos().stream().map(BarbeiroAdminDto::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BarbeiroAdminDto cadastrar(@Valid @RequestBody NomeRequest r) {
        return BarbeiroAdminDto.de(service.cadastrar(r.nome()));
    }

    @PutMapping("/{id}")
    BarbeiroAdminDto renomear(@PathVariable Long id, @Valid @RequestBody NomeRequest r) {
        return BarbeiroAdminDto.de(service.renomear(id, r.nome()));
    }

    @PostMapping("/{id}/desativar")
    BarbeiroAdminDto desativar(@PathVariable Long id) {
        return BarbeiroAdminDto.de(service.desativar(id));
    }

    @PostMapping("/{id}/ativar")
    BarbeiroAdminDto ativar(@PathVariable Long id) {
        return BarbeiroAdminDto.de(service.ativar(id));
    }
}
