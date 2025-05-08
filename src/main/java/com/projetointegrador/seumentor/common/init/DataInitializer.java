package com.projetointegrador.seumentor.common.init;

import com.projetointegrador.seumentor.course.model.CourseArea;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.course.repository.CourseAreaRepository;
import com.projetointegrador.seumentor.course.repository.DisciplineRepository;
import com.projetointegrador.seumentor.user.model.Role;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseAreaRepository courseAreaRepository;
    private final DisciplineRepository disciplineRepository;
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (userRepository.count() == 0) {
            log.info(">>> Base de dados de usuários vazia. Criando usuários iniciais...");

            List<User> usersToCreate = new ArrayList<>();

            User adminUser = User.builder()
                    .firstName("Admin")
                    .lastName("SeuMentor")
                    .email("admin@seumentor.com")
                    .cpf("00000000000")
                    .phone("00000000000")
                    .password(passwordEncoder.encode("!Admin123"))
                    .role(Role.ADMIN)
                    .build();
            usersToCreate.add(adminUser);

            for (int i = 1; i <= 9; i++) {
                String cpf = String.format("%011d", i);
                String phone = String.format("119%08d", i);

                User regularUser = User.builder()
                        .firstName("Usuario")
                        .lastName(String.valueOf(i))
                        .email("usuario" + i + "@seumentor.com")
                        .cpf(cpf)
                        .phone(phone)
                        .password(passwordEncoder.encode("Senha@12" + i)) // Senhas diferentes
                        .role(Role.USER)
                        .build();
                usersToCreate.add(regularUser);
            }

            userRepository.saveAll(usersToCreate);
            log.info(">>> {} usuários iniciais criados com sucesso!", usersToCreate.size());

        } else {
            log.info(">>> Base de dados já contém usuários. Nenhuma ação necessária para usuários.");
        }

        if (courseAreaRepository.count() == 0) {
            log.info(">>> Base de dados de CourseArea vazia. Criando áreas de curso iniciais...");
            List<CourseArea> courseAreasToCreate = Arrays.asList(
                    CourseArea.builder().id(1L).course("Análise e Desenvolvimento de Sistemas").area("BÁSICO I").build(),
                    CourseArea.builder().id(2L).course("Análise e Desenvolvimento de Sistemas").area("BÁSICO II").build(),
                    CourseArea.builder().id(3L).course("Análise e Desenvolvimento de Sistemas").area("IA e DEVOPS").build(),
                    CourseArea.builder().id(4L).course("Análise e Desenvolvimento de Sistemas").area("WEB").build(),
                    CourseArea.builder().id(5L).course("Análise e Desenvolvimento de Sistemas").area("DISPOSITIVOS MÓVEIS").build(),
                    CourseArea.builder().id(6L).course("Análise e Desenvolvimento de Sistemas").area("INTEGRADOR").build(),
                    CourseArea.builder().id(7L).course("Análise e Desenvolvimento de Sistemas").area("OPTATIVAS").build()
            );
            courseAreaRepository.saveAll(courseAreasToCreate);
            log.info(">>> {} áreas de curso iniciais criadas com sucesso!", courseAreasToCreate.size());

            Map<Long, CourseArea> savedCourseAreasMap = courseAreaRepository.findAll().stream()
                    .collect(Collectors.toMap(CourseArea::getId, Function.identity()));

            if (disciplineRepository.count() == 0) {
                log.info(">>> Base de dados de Discipline vazia. Criando disciplinas iniciais...");
                List<Discipline> disciplinesToCreate = Arrays.asList(
                        Discipline.builder().id(1L).disciplineName("ALGORITMOS").description("Introdução à lógica de programação e construção de algoritmos.").courseArea(savedCourseAreasMap.get(1L)).build(),
                        Discipline.builder().id(2L).disciplineName("FUNDAMENTOS DE COMPUTAÇÃO I").description("Conceitos básicos sobre hardware, software e sistemas computacionais.").courseArea(savedCourseAreasMap.get(1L)).build(),
                        Discipline.builder().id(3L).disciplineName("LABORATÓRIO DE PROGRAMAÇÃO").description("Prática de programação e desenvolvimento de software inicial.").courseArea(savedCourseAreasMap.get(1L)).build(),
                        Discipline.builder().id(4L).disciplineName("ENGENHARIA DE SOFTWARE").description("Introdução aos princípios e práticas de engenharia de software.").courseArea(savedCourseAreasMap.get(1L)).build(),
                        Discipline.builder().id(5L).disciplineName("TEOLOGIA, CIENCIAS EXATAS E TECNOLÓGICAS").description("Relação entre teologia e o campo da ciência e tecnologia.").courseArea(savedCourseAreasMap.get(1L)).build(),
                        Discipline.builder().id(6L).disciplineName("ENGENHARIA DE REQUISITOS").description("Técnicas para levantamento, análise e especificação de requisitos de software.").courseArea(savedCourseAreasMap.get(2L)).build(),
                        Discipline.builder().id(7L).disciplineName("FUNDAMENTOS DE PROGRAMAÇÃO ORIENTADA A OBJETO").description("Conceitos e prática da programação orientada a objetos.").courseArea(savedCourseAreasMap.get(2L)).build(),
                        Discipline.builder().id(8L).disciplineName("FUNDAMENTOS DE SISTEMAS DE COMPUTAÇÃO-(EaD)").description("Aprofundamento em sistemas computacionais (modalidade EaD).").courseArea(savedCourseAreasMap.get(2L)).build(),
                        Discipline.builder().id(9L).disciplineName("INTRODUCAO A ESTATISTICA PARA INTELIGENCIA ARTIFICIAL").description("Conceitos estatísticos aplicados à inteligência artificial.").courseArea(savedCourseAreasMap.get(2L)).build(),
                        Discipline.builder().id(10L).disciplineName("PROJETO DE BANCO DE DADOS").description("Modelagem e implementação de bancos de dados relacionais.").courseArea(savedCourseAreasMap.get(2L)).build(),
                        Discipline.builder().id(11L).disciplineName("SEGURANÇA DA INFORMAÇÃO-(EaD)").description("Princípios e práticas de segurança da informação (modalidade EaD).").courseArea(savedCourseAreasMap.get(3L)).build(),
                        Discipline.builder().id(12L).disciplineName("PROCESSOS DE SOFTWARE E GERÊNCIA DE CONFIGURAÇÃO COM DEVOPS").description("Metodologias ágeis, CI/CD e práticas DevOps.").courseArea(savedCourseAreasMap.get(3L)).build(),
                        Discipline.builder().id(13L).disciplineName("ESTRUTURA DE DADOS ORIENTADA A OBJETO").description("Implementação de estruturas de dados usando orientação a objetos.").courseArea(savedCourseAreasMap.get(3L)).build(),
                        Discipline.builder().id(14L).disciplineName("INTELIGENCIA ARTIFICIAL APLICADA").description("Aplicações práticas de técnicas de inteligência artificial.").courseArea(savedCourseAreasMap.get(3L)).build(),
                        Discipline.builder().id(15L).disciplineName("DESENVOLVIMENTO DE SOFTWARE WEB").description("Criação de aplicações web front-end e back-end.").courseArea(savedCourseAreasMap.get(4L)).build(),
                        Discipline.builder().id(16L).disciplineName("MENSAGERIA E STREAMS EM APLICACOES").description("Uso de sistemas de mensageria e processamento de streams.").courseArea(savedCourseAreasMap.get(4L)).build(),
                        Discipline.builder().id(17L).disciplineName("DESIGN DE SOFTWARE").description("Padrões de projeto e princípios de design de software.").courseArea(savedCourseAreasMap.get(4L)).build(),
                        Discipline.builder().id(18L).disciplineName("GERÊNCIA DE QUALIDADE DE SOFTWARE (EaD)").description("Processos e técnicas para garantir a qualidade de software (modalidade EaD).").courseArea(savedCourseAreasMap.get(4L)).build(),
                        Discipline.builder().id(19L).disciplineName("MODELAGEM DE INTERFACES DE USUÁRIO").description("Princípios de design e prototipagem de interfaces de usuário (UI/UX).").courseArea(savedCourseAreasMap.get(4L)).build(),
                        Discipline.builder().id(20L).disciplineName("FERRAMENTAS VISUAIS DE DESENVOLVIMENTO DE SOFTWARE").description("Uso de ferramentas RAD e de baixo código.").courseArea(savedCourseAreasMap.get(5L)).build(),
                        Discipline.builder().id(21L).disciplineName("DESENVOLVIMENTO DE APLICATIVOS P/DISPOSITIVOS MÓVEIS").description("Criação de aplicativos para plataformas móveis (Android/iOS).").courseArea(savedCourseAreasMap.get(5L)).build(),
                        Discipline.builder().id(22L).disciplineName("GOVERNANÇA EM TECNOLOGIA DA INFORMAÇÃO-(EaD)").description("Práticas de gestão e governança de TI (modalidade EaD).").courseArea(savedCourseAreasMap.get(5L)).build(),
                        Discipline.builder().id(23L).disciplineName("PROGRAMAÇÃO ORIENTADA A OBJETOS COM BANCO DE DADOS").description("Integração de aplicações OO com bancos de dados.").courseArea(savedCourseAreasMap.get(5L)).build(),
                        Discipline.builder().id(24L).disciplineName("GERÊNCIA DE PROJETOS DE SISTEMAS-(EaD)").description("Planejamento, execução e controle de projetos de software (modalidade EaD).").courseArea(savedCourseAreasMap.get(5L)).build(),
                        Discipline.builder().id(25L).disciplineName("PROJETO INTEGRADOR").description("Desenvolvimento de um projeto aplicando conhecimentos do curso.").courseArea(savedCourseAreasMap.get(6L)).build(),
                        Discipline.builder().id(26L).disciplineName("INTRODUCAO A BIG DATA E CIENCIA DE DADOS-EAD").description("Conceitos fundamentais de Big Data e Ciência de Dados (modalidade EaD).").courseArea(savedCourseAreasMap.get(6L)).build(),
                        Discipline.builder().id(27L).disciplineName("INTERNET DAS COISAS").description("Princípios e tecnologias de Internet of Things (IoT).").courseArea(savedCourseAreasMap.get(6L)).build(),
                        Discipline.builder().id(28L).disciplineName("NEGÓCIOS em TECNOLOGIA DA INFORMAÇÃO-(EaD)").description("Aspectos de negócios e empreendedorismo em TI (modalidade EaD).").courseArea(savedCourseAreasMap.get(6L)).build(),
                        Discipline.builder().id(29L).disciplineName("OPTATIVA I").description("Disciplina optativa a ser definida conforme escolha do aluno.").courseArea(savedCourseAreasMap.get(6L)).build(),
                        Discipline.builder().id(30L).disciplineName("LIBRAS INSTRUMENTAL").description("Língua Brasileira de Sinais com foco instrumental.").courseArea(savedCourseAreasMap.get(7L)).build(),
                        Discipline.builder().id(31L).disciplineName("INTERPRETAÇÃO DE TEXTO").description("Técnicas de leitura e interpretação textual.").courseArea(savedCourseAreasMap.get(7L)).build(),
                        Discipline.builder().id(32L).disciplineName("PARADIGMAS DE LINGUAGEM DE PROGRAMAÇÃO").description("Estudo de diferentes paradigmas de programação.").courseArea(savedCourseAreasMap.get(7L)).build(),
                        Discipline.builder().id(33L).disciplineName("PROBABILIDADE E ESTATÍSTICA").description("Conceitos avançados de probabilidade e estatística.").courseArea(savedCourseAreasMap.get(7L)).build(),
                        Discipline.builder().id(34L).disciplineName("PRÁTICAS DE DESENVOLVIMENTO DE JOGOS").description("Técnicas e ferramentas para desenvolvimento de jogos.").courseArea(savedCourseAreasMap.get(7L)).build(),
                        Discipline.builder().id(35L).disciplineName("FUNDAMENTOS DE JOGOS").description("Teoria e design de jogos digitais.").courseArea(savedCourseAreasMap.get(7L)).build(),
                        Discipline.builder().id(36L).disciplineName("BANCO DE DADOS II").description("Tópicos avançados em bancos de dados.").courseArea(savedCourseAreasMap.get(7L)).build(),
                        Discipline.builder().id(37L).disciplineName("REDES DE COMPUTADORES I").description("Fundamentos de redes de computadores e protocolos.").courseArea(savedCourseAreasMap.get(7L)).build()
                );
                disciplineRepository.saveAll(disciplinesToCreate);
                log.info(">>> {} disciplinas iniciais criadas com sucesso!", disciplinesToCreate.size());
            } else {
                log.info(">>> Base de dados já contém disciplinas. Nenhuma ação necessária para disciplinas.");
            }
        } else {
            log.info(">>> Base de dados já contém áreas de curso. Nenhuma ação necessária para áreas de curso e disciplinas.");
        }
    }
}