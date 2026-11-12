package br.com.barbearia.adapters.web;

import br.com.barbearia.adapters.web.Dtos.AgendamentoAdminDto;
import br.com.barbearia.application.AgendamentoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/agendamentos")
class AdminController {

    private final AgendamentoService service;

    AdminController(AgendamentoService service) {
        this.service = service;
    }

    @GetMapping
    List<AgendamentoAdminDto> listar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.listarDoDia(data).stream().map(AgendamentoAdminDto::de).toList();
    }

    @PostMapping("/{id}/cancelar")
    AgendamentoAdminDto cancelar(@PathVariable UUID id) {
        return AgendamentoAdminDto.de(service.cancelar(id));
    }

    @PostMapping("/{id}/concluir")
    AgendamentoAdminDto concluir(@PathVariable UUID id) {
        return AgendamentoAdminDto.de(service.concluir(id));
    }
}
