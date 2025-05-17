package com.projetointegrador.seumentor.common.util;

import java.util.InputMismatchException;

public class CPFUtils {

    private CPFUtils() {
    }

    public static String removerFormatacao(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^0-9]", "");
    }

    public static String formatar(String cpfNumeros) {
        if (cpfNumeros == null || cpfNumeros.length() != 11) {
            return cpfNumeros;
        }
        return cpfNumeros.substring(0, 3) + "." +
                cpfNumeros.substring(3, 6) + "." +
                cpfNumeros.substring(6, 9) + "-" +
                cpfNumeros.substring(9, 11);
    }

    public static boolean isValid(String cpf) {
        String cpfLimpo = removerFormatacao(cpf);

        if (cpfLimpo == null || cpfLimpo.length() != 11) {
            return false;
        }

        if (cpfLimpo.matches("(\\d)\\1{10}")) {
            return false;
        }

        char dig10, dig11;
        int sm, i, r, num, peso;

        try {
            sm = 0;
            peso = 10;
            for (i = 0; i < 9; i++) {
                num = cpfLimpo.charAt(i) - '0'; 
                sm = sm + (num * peso);
                peso = peso - 1;
            }

            r = 11 - (sm % 11);
            if ((r == 10) || (r == 11)) {
                dig10 = '0';
            } else {
                dig10 = (char) (r + '0'); 
            }

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

            return (dig10 == cpfLimpo.charAt(9)) && (dig11 == cpfLimpo.charAt(10));

        } catch (InputMismatchException erro) {
            return false;
        }
    }
}