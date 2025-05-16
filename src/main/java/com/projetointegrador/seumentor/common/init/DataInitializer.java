package com.projetointegrador.seumentor.common.init;

import com.projetointegrador.seumentor.course.model.CourseArea;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.course.repository.CourseAreaRepository;
import com.projetointegrador.seumentor.course.repository.DisciplineRepository;
import com.projetointegrador.seumentor.tutoring.model.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;
import com.projetointegrador.seumentor.tutoring.model.TutoringParticipants;
import com.projetointegrador.seumentor.tutoring.repository.TutoringParticipantsRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import com.projetointegrador.seumentor.user.model.DayWeek;
import com.projetointegrador.seumentor.user.model.MentorAvailability;
import com.projetointegrador.seumentor.user.model.Role;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.user.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseAreaRepository courseAreaRepository;
    private final DisciplineRepository disciplineRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository; // Adicionado
    private final TutoringRepository tutoringRepository; // Adicionado
    private final TutoringParticipantsRepository tutoringParticipantsRepository; // Adicionado

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
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("BÁSICO I").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("BÁSICO II").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("IA e DEVOPS").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("WEB").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("DISPOSITIVOS MÓVEIS").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("INTEGRADOR").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("OPTATIVAS").build()
            );
            courseAreaRepository.saveAll(courseAreasToCreate);
            log.info(">>> {} áreas de curso iniciais criadas com sucesso!", courseAreasToCreate.size());

            List<CourseArea> savedCourseAreasMap = courseAreaRepository.findAll();

            if (disciplineRepository.count() == 0) {
                log.info(">>> Base de dados de Discipline vazia. Criando disciplinas iniciais...");
                List<Discipline> disciplinesToCreate;
                disciplinesToCreate = Arrays.asList(
                        Discipline.builder().disciplineName("ALGORITMOS").description("Introdução à lógica de programação e construção de algoritmos.").courseArea(savedCourseAreasMap.getFirst()).build(), // ID 1 esperado
                        Discipline.builder().disciplineName("FUNDAMENTOS DE COMPUTAÇÃO I").description("Conceitos básicos sobre hardware, software e sistemas computacionais.").courseArea(savedCourseAreasMap.getFirst()).build(), // ID 2 esperado
                        Discipline.builder().disciplineName("LABORATÓRIO DE PROGRAMAÇÃO").description("Prática de programação e desenvolvimento de software inicial.").courseArea(savedCourseAreasMap.getFirst()).build(), // ID 3 esperado
                        Discipline.builder().disciplineName("ENGENHARIA DE SOFTWARE").description("Introdução aos princípios e práticas de engenharia de software.").courseArea(savedCourseAreasMap.getFirst()).build(), // ID 4 esperado
                        Discipline.builder().disciplineName("TEOLOGIA, CIENCIAS EXATAS E TECNOLÓGICAS").description("Relação entre teologia e o campo da ciência e tecnologia.").courseArea(savedCourseAreasMap.getFirst()).build(), // ID 5 esperado
                        Discipline.builder().disciplineName("ENGENHARIA DE REQUISITOS").description("Técnicas para levantamento, análise e especificação de requisitos de software.").courseArea(savedCourseAreasMap.get(1)).build(), // ID 6 esperado
                        Discipline.builder().disciplineName("FUNDAMENTOS DE PROGRAMAÇÃO ORIENTADA A OBJETO").description("Conceitos e prática da programação orientada a objetos.").courseArea(savedCourseAreasMap.get(1)).build(),
                        Discipline.builder().disciplineName("FUNDAMENTOS DE SISTEMAS DE COMPUTAÇÃO-(EaD)").description("Aprofundamento em sistemas computacionais (modalidade EaD).").courseArea(savedCourseAreasMap.get(1)).build(),
                        Discipline.builder().disciplineName("INTRODUCAO A ESTATISTICA PARA INTELIGENCIA ARTIFICIAL").description("Conceitos estatísticos aplicados à inteligência artificial.").courseArea(savedCourseAreasMap.get(1)).build(),
                        Discipline.builder().disciplineName("PROJETO DE BANCO DE DADOS").description("Modelagem e implementação de bancos de dados relacionais.").courseArea(savedCourseAreasMap.get(1)).build(),
                        Discipline.builder().disciplineName("SEGURANÇA DA INFORMAÇÃO-(EaD)").description("Princípios e práticas de segurança da informação (modalidade EaD).").courseArea(savedCourseAreasMap.get(2)).build(),
                        Discipline.builder().disciplineName("PROCESSOS DE SOFTWARE E GERÊNCIA DE CONFIGURAÇÃO COM DEVOPS").description("Metodologias ágeis, CI/CD e práticas DevOps.").courseArea(savedCourseAreasMap.get(2)).build(),
                        Discipline.builder().disciplineName("ESTRUTURA DE DADOS ORIENTADA A OBJETO").description("Implementação de estruturas de dados usando orientação a objetos.").courseArea(savedCourseAreasMap.get(2)).build(),
                        Discipline.builder().disciplineName("INTELIGENCIA ARTIFICIAL APLICADA").description("Aplicações práticas de técnicas de inteligência artificial.").courseArea(savedCourseAreasMap.get(2)).build(),
                        Discipline.builder().disciplineName("DESENVOLVIMENTO DE SOFTWARE WEB").description("Criação de aplicações web front-end e back-end.").courseArea(savedCourseAreasMap.get(3)).build(),
                        Discipline.builder().disciplineName("MENSAGERIA E STREAMS EM APLICACOES").description("Uso de sistemas de mensageria e processamento de streams.").courseArea(savedCourseAreasMap.get(3)).build(),
                        Discipline.builder().disciplineName("DESIGN DE SOFTWARE").description("Padrões de projeto e princípios de design de software.").courseArea(savedCourseAreasMap.get(3)).build(),
                        Discipline.builder().disciplineName("GERÊNCIA DE QUALIDADE DE SOFTWARE (EaD)").description("Processos e técnicas para garantir a qualidade de software (modalidade EaD).").courseArea(savedCourseAreasMap.get(3)).build(),
                        Discipline.builder().disciplineName("MODELAGEM DE INTERFACES DE USUÁRIO").description("Princípios de design e prototipagem de interfaces de usuário (UI/UX).").courseArea(savedCourseAreasMap.get(3)).build(),
                        Discipline.builder().disciplineName("FERRAMENTAS VISUAIS DE DESENVOLVIMENTO DE SOFTWARE").description("Uso de ferramentas RAD e de baixo código.").courseArea(savedCourseAreasMap.get(4)).build(),
                        Discipline.builder().disciplineName("DESENVOLVIMENTO DE APLICATIVOS P/DISPOSITIVOS MÓVEIS").description("Criação de aplicativos para plataformas móveis (Android/iOS).").courseArea(savedCourseAreasMap.get(4)).build(),
                        Discipline.builder().disciplineName("GOVERNANÇA EM TECNOLOGIA DA INFORMAÇÃO-(EaD)").description("Práticas de gestão e governança de TI (modalidade EaD).").courseArea(savedCourseAreasMap.get(4)).build(),
                        Discipline.builder().disciplineName("PROGRAMAÇÃO ORIENTADA A OBJETOS COM BANCO DE DADOS").description("Integração de aplicações OO com bancos de dados.").courseArea(savedCourseAreasMap.get(4)).build(),
                        Discipline.builder().disciplineName("GERÊNCIA DE PROJETOS DE SISTEMAS-(EaD)").description("Planejamento, execução e controle de projetos de software (modalidade EaD).").courseArea(savedCourseAreasMap.get(4)).build(),
                        Discipline.builder().disciplineName("PROJETO INTEGRADOR").description("Desenvolvimento de um projeto aplicando conhecimentos do curso.").courseArea(savedCourseAreasMap.get(5)).build(),
                        Discipline.builder().disciplineName("INTRODUCAO A BIG DATA E CIENCIA DE DADOS-EAD").description("Conceitos fundamentais de Big Data e Ciência de Dados (modalidade EaD).").courseArea(savedCourseAreasMap.get(5)).build(),
                        Discipline.builder().disciplineName("INTERNET DAS COISAS").description("Princípios e tecnologias de Internet of Things (IoT).").courseArea(savedCourseAreasMap.get(5)).build(),
                        Discipline.builder().disciplineName("NEGÓCIOS em TECNOLOGIA DA INFORMAÇÃO-(EaD)").description("Aspectos de negócios e empreendedorismo em TI (modalidade EaD).").courseArea(savedCourseAreasMap.get(5)).build(),
                        Discipline.builder().disciplineName("OPTATIVA I").description("Disciplina optativa a ser definida conforme escolha do aluno.").courseArea(savedCourseAreasMap.get(5)).build(),
                        Discipline.builder().disciplineName("LIBRAS INSTRUMENTAL").description("Língua Brasileira de Sinais com foco instrumental.").courseArea(savedCourseAreasMap.get(6)).build(),
                        Discipline.builder().disciplineName("INTERPRETAÇÃO DE TEXTO").description("Técnicas de leitura e interpretação textual.").courseArea(savedCourseAreasMap.get(6)).build(),
                        Discipline.builder().disciplineName("PARADIGMAS DE LINGUAGEM DE PROGRAMAÇÃO").description("Estudo de diferentes paradigmas de programação.").courseArea(savedCourseAreasMap.get(6)).build(),
                        Discipline.builder().disciplineName("PROBABILIDADE E ESTATÍSTICA").description("Conceitos avançados de probabilidade e estatística.").courseArea(savedCourseAreasMap.get(6)).build(),
                        Discipline.builder().disciplineName("PRÁTICAS DE DESENVOLVIMENTO DE JOGOS").description("Técnicas e ferramentas para desenvolvimento de jogos.").courseArea(savedCourseAreasMap.get(6)).build(),
                        Discipline.builder().disciplineName("FUNDAMENTOS DE JOGOS").description("Teoria e design de jogos digitais.").courseArea(savedCourseAreasMap.get(6)).build(),
                        Discipline.builder().disciplineName("BANCO DE DADOS II").description("Tópicos avançados em bancos de dados.").courseArea(savedCourseAreasMap.get(6)).build(),
                        Discipline.builder().disciplineName("REDES DE COMPUTADORES I").description("Fundamentos de redes de computadores e protocolos.").courseArea(savedCourseAreasMap.get(6)).build()
                );
                disciplineRepository.saveAll(disciplinesToCreate);
                log.info(">>> {} disciplinas iniciais criadas com sucesso!", disciplinesToCreate.size());
            } else {
                log.info(">>> Base de dados já contém disciplinas. Nenhuma ação necessária para disciplinas.");
            }
        } else {
            log.info(">>> Base de dados já contém áreas de curso. Nenhuma ação necessária para áreas de curso e disciplinas.");
        }

        // Bloco para criar dados de teste para visualização no front-end
        // Verifica se as mentorias de teste já foram criadas para evitar duplicidade
        if (tutoringRepository.count() < 2 && userRepository.findByEmail("ana.oliveira@example.com").isEmpty()) {
            log.info(">>> Criando dados de teste para mentoria (Ana Julia e Bruno Costa)...");

            // --- Usuário 1: Ana Julia ---
            User anaJulia = User.builder()
                    .firstName("Ana Julia")
                    .lastName("Oliveira")
                    .email("ana.oliveira@example.com")
                    .cpf("11122233344")
                    .phone("11912345678")
                    .password(passwordEncoder.encode("Senha@123"))
                    .role(Role.MENTOR)
                    .profileImg("https://example.com/ana_perfil.jpg")
                    .birthday("1990-05-15")
                    .city("São Paulo")
                    .state("SP")
                    .country("Brasil")
                    .courseName("Ciência da Computação")
                    .semester("7")
                    .university("Universidade Exemplo Alfa")
                    .build();
            userRepository.save(anaJulia);
            log.info(">>> Usuária Ana Julia criada com ID: {}", anaJulia.getId());

            // Disponibilidades de Ana Julia
            Optional<Discipline> discAlgoritmosOpt = disciplineRepository.findById(1L); // ALGORITMOS
            Optional<Discipline> discFundCompIOpt = disciplineRepository.findById(2L); // FUNDAMENTOS DE COMPUTAÇÃO I
            Optional<Discipline> discLabProgOpt = disciplineRepository.findById(3L); // LABORATÓRIO DE PROGRAMAÇÃO

            if (discAlgoritmosOpt.isPresent()) {
                MentorAvailability availAna1 = MentorAvailability.builder()
                        .user(anaJulia)
                        .discipline(discAlgoritmosOpt.get())
                        .dayOfWeek(DayWeek.SEGUNDA_FEIRA)
                        .startTime(LocalTime.of(10, 0, 0))
                        .endTime(LocalTime.of(12, 0, 0))
                        .tutoringClassType(TutoringClassType.ONLINE)
                        .isAvailable(true)
                        .build();
                mentorAvailabilityRepository.save(availAna1);
            } else { log.warn("Disciplina com ID 1L não encontrada para disponibilidade de Ana Julia."); }

            if (discFundCompIOpt.isPresent()) {
                MentorAvailability availAna2 = MentorAvailability.builder()
                        .user(anaJulia)
                        .discipline(discFundCompIOpt.get())
                        .dayOfWeek(DayWeek.QUARTA_FEIRA)
                        .startTime(LocalTime.of(14, 0, 0))
                        .endTime(LocalTime.of(16, 0, 0))
                        .tutoringClassType(TutoringClassType.PRESENCIAL)
                        .isAvailable(true)
                        .build();
                mentorAvailabilityRepository.save(availAna2);
            } else { log.warn("Disciplina com ID 2L não encontrada para disponibilidade de Ana Julia."); }

            if (discLabProgOpt.isPresent()) {
                MentorAvailability availAna3 = MentorAvailability.builder()
                        .user(anaJulia)
                        .discipline(discLabProgOpt.get())
                        .dayOfWeek(DayWeek.SEXTA_FEIRA)
                        .startTime(LocalTime.of(9, 0, 0))
                        .endTime(LocalTime.of(11, 0, 0))
                        .tutoringClassType(TutoringClassType.ONLINE)
                        .isAvailable(true)
                        .build();
                mentorAvailabilityRepository.save(availAna3);
                log.info(">>> Disponibilidades de Ana Julia criadas.");
            } else { log.warn("Disciplina com ID 3L não encontrada para disponibilidade de Ana Julia."); }


            // --- Usuário 2: Bruno Costa ---
            User brunoCosta = User.builder()
                    .firstName("Bruno Costa")
                    .lastName("Santos")
                    .email("bruno.santos@example.com")
                    .cpf("22233344455")
                    .phone("21987654321")
                    .password(passwordEncoder.encode("Senha@456"))
                    .role(Role.MENTOR)
                    .profileImg("https://example.com/bruno_perfil.jpg")
                    .birthday("1992-08-20")
                    .city("Rio de Janeiro")
                    .state("RJ")
                    .country("Brasil")
                    .courseName("Engenharia de Software")
                    .semester("5")
                    .university("Universidade Exemplo Beta")
                    .build();
            userRepository.save(brunoCosta);
            log.info(">>> Usuário Bruno Costa criado com ID: {}", brunoCosta.getId());

            // Disponibilidades de Bruno Costa
            Optional<Discipline> discEngSoftwareOpt = disciplineRepository.findById(4L); // ENGENHARIA DE SOFTWARE
            Optional<Discipline> discTeologiaOpt = disciplineRepository.findById(5L); // TEOLOGIA, CIENCIAS EXATAS E TECNOLÓGICAS
            Optional<Discipline> discEngRequisitosOpt = disciplineRepository.findById(6L); // ENGENHARIA DE REQUISITOS

            if (discEngSoftwareOpt.isPresent()) {
                MentorAvailability availBruno1 = MentorAvailability.builder()
                        .user(brunoCosta)
                        .discipline(discEngSoftwareOpt.get())
                        .dayOfWeek(DayWeek.TERCA_FEIRA)
                        .startTime(LocalTime.of(13, 0, 0))
                        .endTime(LocalTime.of(15, 0, 0))
                        .tutoringClassType(TutoringClassType.PRESENCIAL)
                        .isAvailable(true)
                        .build();
                mentorAvailabilityRepository.save(availBruno1);
            } else { log.warn("Disciplina com ID 4L não encontrada para disponibilidade de Bruno Costa."); }

            if (discTeologiaOpt.isPresent()) {
                MentorAvailability availBruno2 = MentorAvailability.builder()
                        .user(brunoCosta)
                        .discipline(discTeologiaOpt.get())
                        .dayOfWeek(DayWeek.QUINTA_FEIRA)
                        .startTime(LocalTime.of(18, 0, 0))
                        .endTime(LocalTime.of(20, 0, 0))
                        .tutoringClassType(TutoringClassType.ONLINE)
                        .isAvailable(true)
                        .build();
                mentorAvailabilityRepository.save(availBruno2);
            } else { log.warn("Disciplina com ID 5L não encontrada para disponibilidade de Bruno Costa."); }

            if (discEngRequisitosOpt.isPresent()) {
                MentorAvailability availBruno3 = MentorAvailability.builder()
                        .user(brunoCosta)
                        .discipline(discEngRequisitosOpt.get())
                        .dayOfWeek(DayWeek.SABADO)
                        .startTime(LocalTime.of(10, 0, 0))
                        .endTime(LocalTime.of(12, 0, 0))
                        .tutoringClassType(TutoringClassType.ONLINE)
                        .isAvailable(true)
                        .build();
                mentorAvailabilityRepository.save(availBruno3);
                log.info(">>> Disponibilidades de Bruno Costa criadas.");
            } else { log.warn("Disciplina com ID 6L não encontrada para disponibilidade de Bruno Costa."); }


            // --- Mentorias ---
            // Mentoria 1: Ana (Mentor) para Bruno (Mentorado)
            if (discAlgoritmosOpt.isPresent()) {
                Tutoring mentoriaAnaParaBruno = Tutoring.builder()
                        .mentor(anaJulia)
                        .discipline(discAlgoritmosOpt.get())
                        .tutoringClassType(TutoringClassType.ONLINE)
                        .status(StatusTutoring.AGENDADA)
                        .tutoringDate(LocalDate.of(2025, 5, 19))
                        .startTime(LocalTime.of(10, 0, 0))
                        .endTime(LocalTime.of(11, 0, 0))
                        .linkVideo("https://meet.example.com/ana-mentoria-bruno")
                        .maxParticipants(5)
                        .isChatEnable(true)
                        .build();
                tutoringRepository.save(mentoriaAnaParaBruno);

                TutoringParticipants participanteBruno = TutoringParticipants.builder()
                        .tutoring(mentoriaAnaParaBruno)
                        .user(brunoCosta)
                        .topic("Dúvidas sobre complexidade de algoritmos.")
                        .build();
                tutoringParticipantsRepository.save(participanteBruno);
                log.info(">>> Mentoria de Ana para Bruno criada.");
            } else { log.warn("Não foi possível criar mentoria de Ana para Bruno: Disciplina ALGORITMOS (ID 1L) não encontrada."); }


            // Mentoria 2: Bruno (Mentor) para Ana (Mentorado)
            if (discEngSoftwareOpt.isPresent()) {
                Tutoring mentoriaBrunoParaAna = Tutoring.builder()
                        .mentor(brunoCosta)
                        .discipline(discEngSoftwareOpt.get())
                        .tutoringClassType(TutoringClassType.PRESENCIAL)
                        .status(StatusTutoring.AGENDADA)
                        .tutoringDate(LocalDate.of(2025, 5, 20))
                        .startTime(LocalTime.of(13, 0, 0))
                        .endTime(LocalTime.of(14, 0, 0))
                        .local("Sala de Reuniões 3, Campus Central")
                        .maxParticipants(3)
                        .isChatEnable(false)
                        .build();
                tutoringRepository.save(mentoriaBrunoParaAna);

                TutoringParticipants participanteAna = TutoringParticipants.builder()
                        .tutoring(mentoriaBrunoParaAna)
                        .user(anaJulia)
                        .topic("Discussão sobre metodologias ágeis.")
                        .build();
                tutoringParticipantsRepository.save(participanteAna);
                log.info(">>> Mentoria de Bruno para Ana criada.");
            } else { log.warn("Não foi possível criar mentoria de Bruno para Ana: Disciplina ENGENHARIA DE SOFTWARE (ID 4L) não encontrada."); }

            log.info(">>> Dados de teste para mentoria criados com sucesso!");
        } else {
            log.info(">>> Dados de teste para mentoria (Ana Julia e Bruno Costa) já existem ou condição não satisfeita. Nenhuma ação necessária.");
        }
    }
}