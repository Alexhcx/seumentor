package com.projetointegrador.seumentor.common.util;

import java.util.InputMismatchException;

public class CPFUtils {

    private CPFUtils() {
    }

    /**
     * Remove caracteres não numéricos de uma string de CPF.
     *
     * @param cpf O CPF com ou sem formatação.
     * @return O CPF contendo apenas dígitos, ou null se o CPF de entrada for null.
     */
    public static String removerFormatacao(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^0-9]", "");
    }

    /**
     * Formata um CPF (apenas dígitos) para o padrão XXX.XXX.XXX-XX.
     *
     * @param cpfNumeros O CPF contendo apenas 11 dígitos.
     * @return O CPF formatado, ou o CPF original se não tiver 11 dígitos.
     */
    public static String formatar(String cpfNumeros) {
        if (cpfNumeros == null || cpfNumeros.length() != 11) {
            return cpfNumeros; // Retorna original se não for um CPF de 11 dígitos
        }
        return cpfNumeros.substring(0, 3) + "." +
                cpfNumeros.substring(3, 6) + "." +
                cpfNumeros.substring(6, 9) + "-" +
                cpfNumeros.substring(9, 11);
    }

    /**
     * Valida um CPF brasileiro.
     *
     * @param cpf O CPF a ser validado, pode estar formatado ou não.
     * @return true se o CPF for válido, false caso contrário.
     */
    public static boolean isValid(String cpf) {
        String cpfLimpo = removerFormatacao(cpf);

        if (cpfLimpo == null || cpfLimpo.length() != 11) {
            return false;
        }

        // Verifica se todos os dígitos são iguais (ex: 00000000000, 11111111111)
        if (cpfLimpo.matches("(\\d)\\1{10}")) {
            return false;
        }

        char dig10, dig11;
        int sm, i, r, num, peso;

        try {
            // Cálculo do 1º Dígito Verificador
            sm = 0;
            peso = 10;
            for (i = 0; i < 9; i++) {
                num = cpfLimpo.charAt(i) - '0'; // Converte char para int
                sm = sm + (num * peso);
                peso = peso - 1;
            }

            r = 11 - (sm % 11);
            if ((r == 10) || (r == 11)) {
                dig10 = '0';
            } else {
                dig10 = (char) (r + '0'); // Converte int para char
            }

            // Cálculo do 2º Dígito Verificador
            sm = 0;
            peso = 11;
            for (i = 0; i < 10; i++) {
                num = cpfLimpo.charAt(i) - '0';
                sm = sm + (num * peso);
                peso = peso - 1;
            }

            r = 11 - (sm % 11);
            if ((r == 10) || (r == 11)) {
                dig11 = '0';
            } else {
                dig11 = (char) (r + '0');
            }

            // Verifica se os dígitos calculados conferem com os dígitos informados
            return (dig10 == cpfLimpo.charAt(9)) && (dig11 == cpfLimpo.charAt(10));

        } catch (InputMismatchException erro) {
            return false;
        }
    }
}