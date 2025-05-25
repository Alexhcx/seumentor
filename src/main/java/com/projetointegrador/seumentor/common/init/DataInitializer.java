package com.projetointegrador.seumentor.common.init;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.course.model.CourseArea;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.course.repository.CourseAreaRepository;
import com.projetointegrador.seumentor.course.repository.DisciplineRepository;
import com.projetointegrador.seumentor.tutoring.model.MentorAvailability;
import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;
import com.projetointegrador.seumentor.tutoring.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.user.enums.Role;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseAreaRepository courseAreaRepository;
    private final DisciplineRepository disciplineRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository;

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeUsers();
        initializeCourseAreasAndDisciplines();
        initializeMentorAvailabilities();
    }

    private void initializeUsers() {
        if (userRepository.count() == 0) {
            log.info(">>> Base de dados de usuários vazia. Criando usuários iniciais...");
            List<User> usersToCreate = new ArrayList<>();

            // Admin User
            User adminUser = User.builder()
                    .firstName("Admin")
                    .lastName("SeuMentor")
                    .email("admin@seumentor.com")
                    .cpf("00000000000") // Unique CPF for admin
                    .phone("00000000000") // Unique phone for admin
                    .password(passwordEncoder.encode("!Admin123"))
                    .role(Role.ADMIN)
                    .city("Goiânia")
                    .state("GO")
                    .country("Brasil")
                    .build();
            usersToCreate.add(adminUser);

            String[] firstNames = {
                    "Alice", "Bob", "Carlos", "Diana", "Eduardo", "Fernanda", "Gabriel", "Helena",
                    "Igor", "Julia", "Lucas", "Mariana", "Nelson", "Olivia", "Pedro", "Quintino",
                    "Raquel", "Samuel", "Tatiana", "Ulisses", "Valentina", "Wagner", "Xuxa", "Yasmin", "Ziraldo",

                    "Antônio", "José", "Francisco", "João", "Marcos", "Paulo", "Rafael", "Bruno",
                    "Felipe", "Gustavo", "André", "Ricardo", "Fernando", "Daniel", "Diego", "Thiago", "Leonardo",
                    "Maria", "Ana", "Adriana", "Juliana", "Márcia", "Patricia", "Aline", "Sandra",
                    "Camila", "Amanda", "Bruna", "Jéssica", "Letícia", "Beatriz", "Luana", "Vanessa"
            };
            String[] lastNames = {
                    "Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Alves", "Pereira",
                    "Lima", "Gomes", "Costa", "Ribeiro", "Martins", "Carvalho", "Almeida", "Lopes",
                    "Fernandes", "Gonçalves", "Mendes", "Nunes", "Cardoso", "Teixeira", "Correia",
                    "Vieira", "Barbosa", "Moraes", "Pinto", "Freitas", "Dias", "Barros"
            };
            for (int i = 1; i <= 49; i++) {
                String firstName = firstNames[random.nextInt(firstNames.length)];
                String lastName = lastNames[random.nextInt(lastNames.length)];
                // Ensure unique CPF and phone for each user
                String cpf = String.format("1%010d", i); // Start with 1 to avoid conflict with admin
                String phone = String.format("629%08d", i); // Goiânia DDD + sequential

                User regularUser = User.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email("user." + firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@example.com")
                        .cpf(cpf)
                        .phone(phone)
                        .password(passwordEncoder.encode("Pass@word" + i)) // Unique password
                        .role(Role.USER) // Initially USER, can be promoted to MENTOR later
                        .city("Goiânia")
                        .state("GO")
                        .country("Brasil")
                        .birthday(String.format("%d-%02d-%02d", 1980 + random.nextInt(25), 1 + random.nextInt(12),
                                1 + random.nextInt(28))) // Random birthday
                        .profileImg("https://i.pravatar.cc/150?u=" + cpf) // Unique avatar based on CPF
                        .courseName((i % 2 == 0) ? "Análise e Desenvolvimento de Sistemas" : "Engenharia de Software")
                        .semester(String.valueOf(1 + random.nextInt(8)))
                        .university((i % 2 == 0) ? "Universidade Federal de Goiás"
                                : "Pontifícia Universidade Católica de Goiás")
                        .build();
                usersToCreate.add(regularUser);
            }

            userRepository.saveAll(usersToCreate);
            log.info(">>> {} usuários iniciais criados com sucesso!", usersToCreate.size());
        } else {
            log.info(">>> Base de dados já contém usuários. Nenhuma ação necessária para usuários.");
        }
    }

    private void initializeCourseAreasAndDisciplines() {
        if (courseAreaRepository.count() == 0) {
            log.info(">>> Base de dados de CourseArea vazia. Criando áreas de curso iniciais...");
            List<CourseArea> courseAreasToCreate = Arrays.asList(
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("BÁSICO I").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("BÁSICO II").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("IA e DEVOPS").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("WEB").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("DISPOSITIVOS MÓVEIS")
                            .build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("INTEGRADOR").build(),
                    CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("OPTATIVAS").build());
            courseAreaRepository.saveAll(courseAreasToCreate);
            log.info(">>> {} áreas de curso iniciais criadas com sucesso!", courseAreasToCreate.size());

            // Fetch saved course areas to get their IDs
            List<CourseArea> savedCourseAreas = courseAreaRepository.findAll();
            if (savedCourseAreas.isEmpty() || savedCourseAreas.size() < 7) {
                log.error(">>> Erro ao buscar áreas de curso salvas. Não é possível criar disciplinas.");
                return;
            }

            if (disciplineRepository.count() == 0) {
                log.info(">>> Base de dados de Discipline vazia. Criando disciplinas iniciais...");
                List<Discipline> disciplinesToCreate = Arrays.asList(
                        Discipline.builder().disciplineName("ALGORITMOS")
                                .description("Introdução à lógica de programação e construção de algoritmos.")
                                .courseArea(savedCourseAreas.get(0)).build(),
                        Discipline.builder().disciplineName("FUNDAMENTOS DE COMPUTAÇÃO I")
                                .description("Conceitos básicos sobre hardware, software e sistemas computacionais.")
                                .courseArea(savedCourseAreas.get(0)).build(),
                        Discipline.builder().disciplineName("LABORATÓRIO DE PROGRAMAÇÃO")
                                .description("Prática de programação e desenvolvimento de software inicial.")
                                .courseArea(savedCourseAreas.get(0)).build(),
                        Discipline.builder().disciplineName("ENGENHARIA DE SOFTWARE")
                                .description("Introdução aos princípios e práticas de engenharia de software.")
                                .courseArea(savedCourseAreas.get(0)).build(),
                        Discipline.builder().disciplineName("TEOLOGIA, CIENCIAS EXATAS E TECNOLÓGICAS")
                                .description("Relação entre teologia e o campo da ciência e tecnologia.")
                                .courseArea(savedCourseAreas.get(0)).build(),
                        Discipline.builder().disciplineName("ENGENHARIA DE REQUISITOS").description(
                                "Técnicas para levantamento, análise e especificação de requisitos de software.")
                                .courseArea(savedCourseAreas.get(1)).build(),
                        Discipline.builder().disciplineName("FUNDAMENTOS DE PROGRAMAÇÃO ORIENTADA A OBJETO")
                                .description("Conceitos e prática da programação orientada a objetos.")
                                .courseArea(savedCourseAreas.get(1)).build(),
                        Discipline.builder().disciplineName("FUNDAMENTOS DE SISTEMAS DE COMPUTAÇÃO-(EaD)")
                                .description("Aprofundamento em sistemas computacionais (modalidade EaD).")
                                .courseArea(savedCourseAreas.get(1)).build(),
                        Discipline.builder().disciplineName("INTRODUCAO A ESTATISTICA PARA INTELIGENCIA ARTIFICIAL")
                                .description("Conceitos estatísticos aplicados à inteligência artificial.")
                                .courseArea(savedCourseAreas.get(1)).build(),
                        Discipline.builder().disciplineName("PROJETO DE BANCO DE DADOS")
                                .description("Modelagem e implementação de bancos de dados relacionais.")
                                .courseArea(savedCourseAreas.get(1)).build(),
                        Discipline.builder().disciplineName("SEGURANÇA DA INFORMAÇÃO-(EaD)")
                                .description("Princípios e práticas de segurança da informação (modalidade EaD).")
                                .courseArea(savedCourseAreas.get(2)).build(),
                        Discipline.builder()
                                .disciplineName("PROCESSOS DE SOFTWARE E GERÊNCIA DE CONFIGURAÇÃO COM DEVOPS")
                                .description("Metodologias ágeis, CI/CD e práticas DevOps.")
                                .courseArea(savedCourseAreas.get(2)).build(),
                        Discipline.builder().disciplineName("ESTRUTURA DE DADOS ORIENTADA A OBJETO")
                                .description("Implementação de estruturas de dados usando orientação a objetos.")
                                .courseArea(savedCourseAreas.get(2)).build(),
                        Discipline.builder().disciplineName("INTELIGENCIA ARTIFICIAL APLICADA")
                                .description("Aplicações práticas de técnicas de inteligência artificial.")
                                .courseArea(savedCourseAreas.get(2)).build(),
                        Discipline.builder().disciplineName("DESENVOLVIMENTO DE SOFTWARE WEB")
                                .description("Criação de aplicações web front-end e back-end.")
                                .courseArea(savedCourseAreas.get(3)).build(),
                        Discipline.builder().disciplineName("MENSAGERIA E STREAMS EM APLICACOES")
                                .description("Uso de sistemas de mensageria e processamento de streams.")
                                .courseArea(savedCourseAreas.get(3)).build(),
                        Discipline.builder().disciplineName("DESIGN DE SOFTWARE")
                                .description("Padrões de projeto e princípios de design de software.")
                                .courseArea(savedCourseAreas.get(3)).build(),
                        Discipline.builder().disciplineName("GERÊNCIA DE QUALIDADE DE SOFTWARE (EaD)")
                                .description(
                                        "Processos e técnicas para garantir a qualidade de software (modalidade EaD).")
                                .courseArea(savedCourseAreas.get(3)).build(),
                        Discipline.builder().disciplineName("MODELAGEM DE INTERFACES DE USUÁRIO")
                                .description("Princípios de design e prototipagem de interfaces de usuário (UI/UX).")
                                .courseArea(savedCourseAreas.get(3)).build(),
                        Discipline.builder().disciplineName("FERRAMENTAS VISUAIS DE DESENVOLVIMENTO DE SOFTWARE")
                                .description("Uso de ferramentas RAD e de baixo código.")
                                .courseArea(savedCourseAreas.get(4)).build(),
                        Discipline.builder().disciplineName("DESENVOLVIMENTO DE APLICATIVOS P/DISPOSITIVOS MÓVEIS")
                                .description("Criação de aplicativos para plataformas móveis (Android/iOS).")
                                .courseArea(savedCourseAreas.get(4)).build(),
                        Discipline.builder().disciplineName("GOVERNANÇA EM TECNOLOGIA DA INFORMAÇÃO-(EaD)")
                                .description("Práticas de gestão e governança de TI (modalidade EaD).")
                                .courseArea(savedCourseAreas.get(4)).build(),
                        Discipline.builder().disciplineName("PROGRAMAÇÃO ORIENTADA A OBJETOS COM BANCO DE DADOS")
                                .description("Integração de aplicações OO com bancos de dados.")
                                .courseArea(savedCourseAreas.get(4)).build(),
                        Discipline.builder().disciplineName("GERÊNCIA DE PROJETOS DE SISTEMAS-(EaD)")
                                .description(
                                        "Planejamento, execução e controle de projetos de software (modalidade EaD).")
                                .courseArea(savedCourseAreas.get(4)).build(),
                        Discipline.builder().disciplineName("PROJETO INTEGRADOR")
                                .description("Desenvolvimento de um projeto aplicando conhecimentos do curso.")
                                .courseArea(savedCourseAreas.get(5)).build(),
                        Discipline.builder().disciplineName("INTRODUCAO A BIG DATA E CIENCIA DE DADOS-EAD")
                                .description("Conceitos fundamentais de Big Data e Ciência de Dados (modalidade EaD).")
                                .courseArea(savedCourseAreas.get(5)).build(),
                        Discipline.builder().disciplineName("INTERNET DAS COISAS")
                                .description("Princípios e tecnologias de Internet of Things (IoT).")
                                .courseArea(savedCourseAreas.get(5)).build(),
                        Discipline.builder().disciplineName("NEGÓCIOS em TECNOLOGIA DA INFORMAÇÃO-(EaD)")
                                .description("Aspectos de negócios e empreendedorismo em TI (modalidade EaD).")
                                .courseArea(savedCourseAreas.get(5)).build(),
                        Discipline.builder().disciplineName("OPTATIVA I")
                                .description("Disciplina optativa a ser definida conforme escolha do aluno.")
                                .courseArea(savedCourseAreas.get(5)).build(),
                        Discipline.builder().disciplineName("LIBRAS INSTRUMENTAL")
                                .description("Língua Brasileira de Sinais com foco instrumental.")
                                .courseArea(savedCourseAreas.get(6)).build(),
                        Discipline.builder().disciplineName("INTERPRETAÇÃO DE TEXTO")
                                .description("Técnicas de leitura e interpretação textual.")
                                .courseArea(savedCourseAreas.get(6)).build(),
                        Discipline.builder().disciplineName("PARADIGMAS DE LINGUAGEM DE PROGRAMAÇÃO")
                                .description("Estudo de diferentes paradigmas de programação.")
                                .courseArea(savedCourseAreas.get(6)).build(),
                        Discipline.builder().disciplineName("PROBABILIDADE E ESTATÍSTICA")
                                .description("Conceitos avançados de probabilidade e estatística.")
                                .courseArea(savedCourseAreas.get(6)).build(),
                        Discipline.builder().disciplineName("PRÁTICAS DE DESENVOLVIMENTO DE JOGOS")
                                .description("Técnicas e ferramentas para desenvolvimento de jogos.")
                                .courseArea(savedCourseAreas.get(6)).build(),
                        Discipline.builder().disciplineName("FUNDAMENTOS DE JOGOS")
                                .description("Teoria e design de jogos digitais.").courseArea(savedCourseAreas.get(6))
                                .build(),
                        Discipline.builder().disciplineName("BANCO DE DADOS II")
                                .description("Tópicos avançados em bancos de dados.")
                                .courseArea(savedCourseAreas.get(6)).build(),
                        Discipline.builder().disciplineName("REDES DE COMPUTADORES I")
                                .description("Fundamentos de redes de computadores e protocolos.")
                                .courseArea(savedCourseAreas.get(6)).build());
                disciplineRepository.saveAll(disciplinesToCreate);
                log.info(">>> {} disciplinas iniciais criadas com sucesso!", disciplinesToCreate.size());
            } else {
                log.info(">>> Base de dados já contém disciplinas. Nenhuma ação necessária para disciplinas.");
            }
        } else {
            log.info(
                    ">>> Base de dados já contém áreas de curso. Nenhuma ação necessária para áreas de curso e disciplinas.");
        }
    }

    private void initializeMentorAvailabilities() {
        if (mentorAvailabilityRepository.count() == 0 && userRepository.count() > 1) { // Ensure users and disciplines
                                                                                       // exist
            log.info(">>> Criando disponibilidades para mentores...");

            List<User> allUsers = userRepository.findAll();
            List<Discipline> allDisciplines = disciplineRepository.findAll();

            if (allDisciplines.isEmpty()) {
                log.warn(">>> Nenhuma disciplina encontrada. Não é possível criar disponibilidades.");
                return;
            }

            // Exclude admin and select 20 users to be mentors
            List<User> potentialMentors = allUsers.stream()
                    .filter(user -> user.getRole() != Role.ADMIN)
                    .collect(Collectors.toList());

            if (potentialMentors.size() < 20) {
                log.warn(
                        ">>> Não há usuários suficientes ({} disponíveis) para selecionar 20 mentores. Ajustando para o número disponível.",
                        potentialMentors.size());
            }

            Collections.shuffle(potentialMentors); // Randomize to pick different mentors each time if run multiple
                                                   // times (though guarded by count)
            List<User> selectedMentors = potentialMentors.stream().limit(20).collect(Collectors.toList());

            List<MentorAvailability> availabilitiesToCreate = new ArrayList<>();
            DayWeek[] days = DayWeek.values();

            for (User mentor : selectedMentors) {
                mentor.setRole(Role.MENTOR); // Promote to MENTOR
                userRepository.save(mentor); // Save role change

                for (Discipline discipline : allDisciplines) {
                    // Create one availability for each mentor-discipline pair
                    // You can vary day, time, and type for more diversity
                    MentorAvailability availability = MentorAvailability.builder()
                            .user(mentor)
                            .discipline(discipline)
                            .dayOfWeek(days[random.nextInt(days.length)]) // Random day
                            .startTime(LocalTime.of(8 + random.nextInt(10), 0)) // Random start time between 8 AM and 5
                                                                                // PM
                            .endTime(LocalTime.of(10 + random.nextInt(10), 0)) // Random end time, ensure end > start
                            .tutoringClassType(
                                    random.nextBoolean() ? TutoringClassType.ONLINE : TutoringClassType.PRESENCIAL) // Random
                                                                                                                    // type
                            .isAvailable(true) // Default to available
                            .build();
                    // Ensure endTime is after startTime
                    if (availability.getEndTime().isBefore(availability.getStartTime())
                            || availability.getEndTime().equals(availability.getStartTime())) {
                        availability.setEndTime(availability.getStartTime().plusHours(2));
                    }

                    availabilitiesToCreate.add(availability);
                }
            }

            if (!availabilitiesToCreate.isEmpty()) {
                mentorAvailabilityRepository.saveAll(availabilitiesToCreate);
                log.info(">>> {} disponibilidades de mentores criadas com sucesso para {} mentores.",
                        availabilitiesToCreate.size(), selectedMentors.size());
            } else {
                log.info(
                        ">>> Nenhuma disponibilidade de mentor foi criada (talvez não haja mentores ou disciplinas suficientes).");
            }

        } else {
            log.info(
                    ">>> Disponibilidades de mentores já existem ou não há usuários/disciplinas suficientes. Nenhuma ação necessária.");
        }
    }
}