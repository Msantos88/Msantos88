package br.com.barbearia.adapters.web;

import br.com.barbearia.adapters.web.Dtos.ErroDto;
import br.com.barbearia.application.NaoEncontradoException;
import br.com.barbearia.application.RegraDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
class TratamentoDeErros {

    @ExceptionHandler(RegraDeNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErroDto regra(RegraDeNegocioException e) {
        return new ErroDto(e.getMessage());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErroDto naoEncontrado(NaoEncontradoException e) {
        return new ErroDto(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErroDto validacao(MethodArgumentNotValidException e) {
        return new ErroDto("Dados inválidos: " + e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " (" + f.getDefaultMessage() + ")")
                .collect(Collectors.joining(", ")));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErroDto requisicaoInvalida(Exception e) {
        return new ErroDto("Requisição inválida");
    }
}
