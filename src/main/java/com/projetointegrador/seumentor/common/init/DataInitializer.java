package com.projetointegrador.seumentor.common.init;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.common.util.CPFUtils;
import com.projetointegrador.seumentor.course.model.CourseArea;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.course.repository.CourseAreaRepository;
import com.projetointegrador.seumentor.course.repository.DisciplineRepository;
import com.projetointegrador.seumentor.tutoring.model.MentorAvailability;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;
import com.projetointegrador.seumentor.tutoring.model.TutoringParticipants;
import com.projetointegrador.seumentor.tutoring.model.TutoringRating;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringParticipantsRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRatingRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final CourseAreaRepository courseAreaRepository;
        private final DisciplineRepository disciplineRepository;
        private final MentorAvailabilityRepository mentorAvailabilityRepository;
        private final TutoringRepository tutoringRepository;
        private final TutoringParticipantsRepository tutoringParticipantsRepository;
        private final TutoringRatingRepository tutoringRatingRepository;

        private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
        private static final DateTimeFormatter SQL_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

        // Maps to store SQL ID -> Actual Entity Object
        private final Map<Long, User> sqlUserIdToUserObjectMap = new HashMap<>();
        private final Map<Long, Discipline> sqlDisciplineIdToDisciplineObjectMap = new HashMap<>();
        private final Map<Long, Tutoring> sqlTutoringIdToTutoringObjectMap = new HashMap<>();

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                initializeUsersFromSqlData();
                initializeCourseAreasAndDisciplines(); // Assumes this creates disciplines with predictable IDs or
                                                       // allows fetching
                                                       // them
                mapSqlDisciplineIdsToEntities(); // Map SQL discipline IDs to actual persisted disciplines
                initializeMentorAvailabilitiesFromSqlData();
                initializeTutoringsAndParticipantsAndRatingsFromSqlData();
        }

        private void initializeUsersFromSqlData() {
                if (userRepository.count() == 0) {
                        log.info(">>> Base de dados de usuários vazia. Criando usuários a partir dos dados SQL...");
                        List<User> usersToCreate = new ArrayList<>();

                        // Admin User (special case, not in SQL range 1-100 for password)
                        User adminUser = User.builder()
                                        .firstName("Admin")
                                        .lastName("SeuMentor")
                                        .email("admin@seumentor.com")
                                        .cpf(CPFUtils.removerFormatacao("000.000.000-00"))
                                        .phone("00000000000")
                                        .password(passwordEncoder.encode("!Admin123"))
                                        .role(Role.ADMIN)
                                        .city("Goiânia")
                                        .state("GO")
                                        .country("Brasil")
                                        .profileImg("https://example.com/imagem_perfil/admin.png")
                                        .build();
                        User savedAdmin = userRepository.save(adminUser); // Save admin first to ensure it has an ID
                        // No SQL ID for admin in the provided list, so not adding to
                        // sqlUserIdToUserObjectMap unless specified

                        // USER Roles (SQL IDs 1-70)
                        String[][] userData = {
                                        // João Silva ... (Copy all 70 USER entries here)
                                        { "1", "1989-07-10", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "alexandrenazareth@hotmail.com", "Alexandre", "Nazareth", "123.456.789-01",
                                                        "(62) 91234-5601",
                                                        "https://seu-mentor-cloud-storage.s3.sa-east-1.amazonaws.com/profile-images/6194b090-c2d4-4540-8131-76f2001da37f.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250608T094236Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAV7D2POADRRV7PTPG%2F20250608%2Fsa-east-1%2Fs3%2Faws4_request&X-Amz-Expires=604800&X-Amz-Signature=b122e77fc7ec69e726ce17eb9b0fe18aacd69458c8ba91ae85a1e1b4509e8196", 
                                                        "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "2", "1995-08-22", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "ale.hc.df@gmail.com", "Maria", "Oliveira",
                                                        "234.567.890-12", "(62) 92345-6702",
                                                        "https://seu-mentor-cloud-storage.s3.sa-east-1.amazonaws.com/profile-images/83b3c8c2-85bf-4016-a022-84e32286eb9f.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250608T095357Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAV7D2POADRRV7PTPG%2F20250608%2Fsa-east-1%2Fs3%2Faws4_request&X-Amz-Expires=604800&X-Amz-Signature=ee29b1d2152c64723ab395dba87e80602633be46e6a835ff38d39e4d334b0019", 
                                                        "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "3", "2003-02-10", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "pedro.pereira@example.com", "Pedro", "Pereira",
                                                        "345.678.901-23", "(62) 93456-7803",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "4", "1975-03-12", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "ana.costa@example.com", "Ana", "Costa", "456.789.012-34",
                                                        "(62) 94567-8904",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "5", "2001-09-05", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "lucas.martins@example.com", "Lucas", "Martins",
                                                        "567.890.123-45", "(62) 95678-9005",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "6", "1980-06-25", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "sofia.santos@example.com", "Sofia", "Santos", "678.901.234-56",
                                                        "(62) 96789-0106",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "7", "2004-01-20", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "gabriel.ribeiro@example.com", "Gabriel", "Ribeiro",
                                                        "789.012.345-67", "(62) 97890-1207",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "8", "1993-10-11", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "laura.carvalho@example.com", "Laura", "Carvalho",
                                                        "890.123.456-78", "(62) 98901-2308",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "9", "2002-12-03", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "bruno.gomes@example.com", "Bruno", "Gomes", "901.234.567-89",
                                                        "(62) 99012-3409",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "10", "1998-03-28", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "camila.almeida@example.com", "Camila", "Almeida",
                                                        "012.345.678-90", "(62) 90123-4510",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "11", "1978-11-11", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "rafael.barros@example.com", "Rafael", "Barros",
                                                        "112.233.445-51", "(62) 91122-3311",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "7",
                                                        "GO", "PUC-GO" },
                                        { "12", "2000-07-19", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "isabela.fernandes@example.com", "Isabela", "Fernandes",
                                                        "223.344.556-62", "(62) 92233-4412",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "13", "1996-04-02", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "matheus.goncalves@example.com", "Matheus", "Gonçalves",
                                                        "334.455.667-73", "(62) 93344-5513",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "14", "1989-09-25", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "beatriz.moreira@example.com", "Beatriz", "Moreira",
                                                        "445.566.778-84", "(62) 94455-6614",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "15", "2005-02-08", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "gustavo.lopes@example.com", "Gustavo", "Lopes",
                                                        "556.677.889-95", "(62) 95566-7715",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "16", "1991-12-30", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "manuela.lima@example.com", "Manuela", "Lima", "667.788.990-06",
                                                        "(62) 96677-8816",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "7",
                                                        "GO", "PUC-GO" },
                                        { "17", "1982-01-14", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "enzo.ferreira@example.com", "Enzo", "Ferreira",
                                                        "778.899.001-17", "(62) 97788-9917",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "18", "2003-08-03", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "alice.araujo@example.com", "Alice", "Araújo", "889.900.112-28",
                                                        "(62) 98899-0018",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "19", "1997-06-17", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "davi.azevedo@example.com", "Davi", "Azevedo", "990.011.223-39",
                                                        "(62) 99900-1119",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "20", "1970-10-09", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "valentina.barbosa@example.com", "Valentina", "Barbosa",
                                                        "001.122.334-40", "(62) 90011-2220",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "21", "1999-01-25", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "arthur.castro@example.com", "Arthur", "Castro",
                                                        "112.345.678-01", "(62) 91123-4521",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "22", "1986-04-18", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "julia.dias@example.com", "Júlia", "Dias", "223.456.789-12",
                                                        "(62) 92234-5622",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "7",
                                                        "GO", "PUC-GO" },
                                        { "23", "2004-11-07", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "heitor.duarte@example.com", "Heitor", "Duarte",
                                                        "334.567.890-23", "(62) 93345-6723",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "24", "1992-07-01", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "lorena.freitas@example.com", "Lorena", "Freitas",
                                                        "445.678.901-34", "(62) 94456-7824",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "25", "1973-02-14", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "bernardo.garcia@example.com", "Bernardo", "Garcia",
                                                        "556.789.012-45", "(62) 95567-8925",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "26", "2001-08-29", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "luiza.melo@example.com", "Luiza", "Melo", "667.890.123-56",
                                                        "(62) 96678-9026",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "27", "1994-05-04", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "miguel.nunes@example.com", "Miguel", "Nunes", "778.901.234-67",
                                                        "(62) 97789-0127",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "28", "1987-10-21", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "helena.pinto@example.com", "Helena", "Pinto", "889.012.345-78",
                                                        "(62) 98890-1228",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "29", "2005-03-16", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "theo.ramos@example.com", "Théo", "Ramos", "990.123.456-89",
                                                        "(62) 99901-2329",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "30", "1968-06-09", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "clara.rocha@example.com", "Clara", "Rocha", "001.234.567-90",
                                                        "(62) 90012-3430",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "7",
                                                        "GO", "PUC-GO" },
                                        { "31", "1999-07-12", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "samuel.sales@example.com", "Samuel", "Sales", "121.343.565-01",
                                                        "(62) 91213-4331",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "32", "1984-02-22", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "sophia.teixeira@example.com", "Sophia", "Teixeira",
                                                        "232.454.676-12", "(62) 92324-5432",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "33", "2002-10-01", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "benjamin.vieira@example.com", "Benjamin", "Vieira",
                                                        "343.565.787-23", "(62) 93435-6533",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "34", "1977-01-30", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "cecilia.xavier@example.com", "Cecília", "Xavier",
                                                        "454.676.898-34", "(62) 94546-7634",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "35", "2000-09-09", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "daniel.antunes@example.com", "Daniel", "Antunes",
                                                        "565.787.909-45", "(62) 95657-8735",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "36", "1995-12-18", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "emanuelly.borges@example.com", "Emanuelly", "Borges",
                                                        "676.898.010-56", "(62) 96768-9836",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "7",
                                                        "GO", "PUC-GO" },
                                        { "37", "1981-06-20", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "felipe.camargo@example.com", "Felipe", "Camargo",
                                                        "787.909.121-67", "(62) 97879-0937",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "38", "2003-04-05", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "giovanna.cardoso@example.com", "Giovanna", "Cardoso",
                                                        "898.010.232-78", "(62) 98980-1038",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "39", "1972-03-27", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "henrique.cunha@example.com", "Henrique", "Cunha",
                                                        "909.121.343-89", "(62) 99091-2139",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "40", "1997-11-14", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "isadora.dantas@example.com", "Isadora", "Dantas",
                                                        "010.232.454-90", "(62) 90102-3240",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "41", "2000-01-01", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "joaquim.esteves@example.com", "Joaquim", "Esteves",
                                                        "123.321.456-11", "(62) 91233-2141",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "42", "1994-02-02", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "leticia.farias@example.com", "Letícia", "Farias",
                                                        "234.432.567-22", "(62) 92344-3242",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "43", "1983-03-03", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "murilo.fogaca@example.com", "Murilo", "Fogaça",
                                                        "345.543.678-33", "(62) 93455-4343",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "7",
                                                        "GO", "PUC-GO" },
                                        { "44", "2001-04-04", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "nicole.franco@example.com", "Nicole", "Franco",
                                                        "456.654.789-44", "(62) 94566-5444",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "45", "1976-05-05", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "otavio.furtado@example.com", "Otávio", "Furtado",
                                                        "567.765.890-55", "(62) 95677-6545",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "46", "1999-06-06", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "pietro.guimaraes@example.com", "Pietro", "Guimarães",
                                                        "678.876.901-66", "(62) 96788-7646",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "47", "1990-07-07", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "rebeca.henriques@example.com", "Rebeca", "Henriques",
                                                        "789.987.012-77", "(62) 97899-8747",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "48", "2004-08-08", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "sergio.leal@example.com", "Sérgio", "Leal", "890.098.123-88",
                                                        "(62) 98900-9848",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "49", "1969-09-09", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "taina.macedo@example.com", "Tainá", "Macedo", "901.109.234-99",
                                                        "(62) 99011-0949",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "50", "1992-10-10", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "ulisses.machado@example.com", "Ulisses", "Machado",
                                                        "012.210.345-00", "(62) 90122-1050",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "7",
                                                        "GO", "PUC-GO" },
                                        { "51", "1988-01-12", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "viviane.magalhaes@example.com", "Viviane", "Magalhães",
                                                        "122.222.333-11", "(62) 91222-2351",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "52", "2002-02-23", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "wesley.matos@example.com", "Wesley", "Matos", "233.333.444-22",
                                                        "(62) 92333-3452",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "53", "1974-03-14", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "yasmin.medeiros@example.com", "Yasmin", "Medeiros",
                                                        "344.444.555-33", "(62) 93444-4553",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "54", "1996-04-25", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "alexandre.miranda@example.com", "Alexandre", "Miranda",
                                                        "455.555.666-44", "(62) 94555-5654",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "55", "1985-05-16", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "bianca.monteiro@example.com", "Bianca", "Monteiro",
                                                        "566.666.777-55", "(62) 95666-6755",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "56", "2005-06-27", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "cristiano.morais@example.com", "Cristiano", "Morais",
                                                        "677.777.888-66", "(62) 96777-7856",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "57", "1979-07-18", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "debora.nascimento@example.com", "Débora", "Nascimento",
                                                        "788.888.999-77", "(62) 97888-8957",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "7",
                                                        "GO", "PUC-GO" },
                                        { "58", "2000-08-29", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "eduardo.neves@example.com", "Eduardo", "Neves",
                                                        "899.999.000-88", "(62) 98999-9058",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "59", "1993-09-03", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "fabiana.noronha@example.com", "Fabiana", "Noronha",
                                                        "900.000.111-99", "(62) 99000-0159",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "60", "1980-10-24", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "gilberto.pires@example.com", "Gilberto", "Pires",
                                                        "011.111.222-00", "(62) 90111-1260",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "61", "2001-11-15", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "helena.queiroz@example.com", "Helena", "Queiroz",
                                                        "121.212.323-11", "(62) 91212-1361",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "62", "1971-12-06", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "igor.quintana@example.com", "Igor", "Quintana",
                                                        "232.323.434-22", "(62) 92323-2462",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "63", "1998-01-27", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "jaqueline.rezende@example.com", "Jaqueline", "Rezende",
                                                        "343.434.545-33", "(62) 93434-3563",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" },
                                        { "64", "1987-02-18", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "kauan.rodrigues@example.com", "Kauan", "Rodrigues",
                                                        "454.545.656-44", "(62) 94545-4664",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "7",
                                                        "GO", "PUC-GO" },
                                        { "65", "2003-03-11", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "larissa.rosa@example.com", "Larissa", "Rosa", "565.656.767-55",
                                                        "(62) 95656-5765",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "1",
                                                        "GO", "PUC-GO" },
                                        { "66", "1976-04-02", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "leonardo.santana@example.com", "Leonardo", "Santana",
                                                        "676.767.878-66", "(62) 96767-6866",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "4",
                                                        "GO", "PUC-GO" },
                                        { "67", "1999-05-23", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "marcela.simoes@example.com", "Marcela", "Simões",
                                                        "787.878.989-77", "(62) 97878-7967",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "5",
                                                        "GO", "PUC-GO" },
                                        { "68", "1990-06-14", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "nathan.soares@example.com", "Nathan", "Soares",
                                                        "898.989.090-88", "(62) 98989-8068",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "2",
                                                        "GO", "PUC-GO" },
                                        { "69", "2004-07-05", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "olivia.tavares@example.com", "Olívia", "Tavares",
                                                        "909.090.101-99", "(62) 99090-9169",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "6",
                                                        "GO", "PUC-GO" },
                                        { "70", "1967-08-26", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "paulo.trindade@example.com", "Paulo", "Trindade",
                                                        "010.101.212-00", "(62) 90101-0270",
                                                        "https://example.com/imagem_perfil/default.png", "USER", "3",
                                                        "GO", "PUC-GO" }
                        };

                        for (String[] data : userData) {
                                long sqlId = Long.parseLong(data[0]);
                                User user = User.builder()
                                                .firstName(data[6])
                                                .lastName(data[7])
                                                .email(data[5])
                                                .cpf(CPFUtils.removerFormatacao(data[8]))
                                                .phone(data[9].replaceAll("[^0-9]", ""))
                                                .password(passwordEncoder
                                                                .encode("Password00"))
                                                .role(Role.valueOf(data[11]))
                                                .birthday(data[1])
                                                .city(data[2])
                                                .state(data[13])
                                                .country(data[3])
                                                .profileImg(data[10])
                                                .courseName(data[4])
                                                .semester(data[12])
                                                .university(data[14])
                                                .build();
                                usersToCreate.add(user);
                        }

                        // MENTOR Roles (SQL IDs 71-100)
                        String[][] mentorData = {
                                        // Carlos Souza ... (Copy all 30 MENTOR entries here)
                                        { "71", "1988-11-01", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "carlos.souza@example.com", "Carlos", "Souza", "123.111.222-01",
                                                        "(62) 91231-1271",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "7",
                                                        "GO", "PUC-GO" },
                                        { "72", "1999-07-30", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "juliana.costa@example.com", "Juliana", "Costa",
                                                        "234.222.333-02", "(62) 92342-2372",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "6",
                                                        "GO", "PUC-GO" },
                                        { "73", "1992-12-18", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "fernando.alves@example.com", "Fernando", "Alves",
                                                        "345.333.444-03", "(62) 93453-3473",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "7",
                                                        "GO", "PUC-GO" },
                                        { "74", "1997-04-03", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "adriana.goncalves@example.com", "Adriana", "Gonçalves",
                                                        "456.444.555-04", "(62) 94564-4574",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "5",
                                                        "GO", "PUC-GO" },
                                        { "75", "1985-07-07", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "ricardo.gomes@example.com", "Ricardo", "Gomes",
                                                        "567.555.666-05", "(62) 95675-5675",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "4",
                                                        "GO", "PUC-GO" },
                                        { "76", "1990-02-15", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "patricia.dias@example.com", "Patrícia", "Dias",
                                                        "678.666.777-06", "(62) 96786-6776",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "6",
                                                        "GO", "PUC-GO" },
                                        { "77", "1983-05-21", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "marcos.lima@example.com", "Marcos", "Lima", "789.777.888-07",
                                                        "(62) 97897-7877",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "7",
                                                        "GO", "PUC-GO" },
                                        { "78", "1996-08-10", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "simone.ferreira@example.com", "Simone", "Ferreira",
                                                        "890.888.999-08", "(62) 98908-8978",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "5",
                                                        "GO", "PUC-GO" },
                                        { "79", "1978-09-05", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "tiago.araujo@example.com", "Tiago", "Araújo", "901.999.000-09",
                                                        "(62) 99019-9079",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "6",
                                                        "GO", "PUC-GO" },
                                        { "80", "1991-03-26", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "vanessa.azevedo@example.com", "Vanessa", "Azevedo",
                                                        "012.000.111-10", "(62) 90120-0180",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "7",
                                                        "GO", "PUC-GO" },
                                        { "81", "1986-12-04", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "rodrigo.barbosa@example.com", "Rodrigo", "Barbosa",
                                                        "111.222.333-41", "(62) 91112-2381",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "4",
                                                        "GO", "PUC-GO" },
                                        { "82", "1994-10-13", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "regina.castro@example.com", "Regina", "Castro",
                                                        "222.333.444-52", "(62) 92223-3482",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "5",
                                                        "GO", "PUC-GO" },
                                        { "83", "1974-06-19", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "andre.duarte@example.com", "André", "Duarte", "333.444.555-63",
                                                        "(62) 93334-4583",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "7",
                                                        "GO", "PUC-GO" },
                                        { "84", "1989-01-08", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "tatiane.freitas@example.com", "Tatiane", "Freitas",
                                                        "444.555.666-74", "(62) 94445-5684",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "6",
                                                        "GO", "PUC-GO" },
                                        { "85", "1995-11-28", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "vinicius.garcia@example.com", "Vinícius", "Garcia",
                                                        "555.666.777-85", "(62) 95556-6785",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "4",
                                                        "GO", "PUC-GO" },
                                        { "86", "1982-04-17", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "erica.melo@example.com", "Érica", "Melo", "666.777.888-96",
                                                        "(62) 96667-7886",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "5",
                                                        "GO", "PUC-GO" },
                                        { "87", "1970-08-02", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "leandro.nunes@example.com", "Leandro", "Nunes",
                                                        "777.888.999-07", "(62) 97778-8987",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "7",
                                                        "GO", "PUC-GO" },
                                        { "88", "1993-07-23", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "sandra.pinto@example.com", "Sandra", "Pinto", "888.999.000-18",
                                                        "(62) 98889-9088",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "6",
                                                        "GO", "PUC-GO" },
                                        { "89", "1987-05-09", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "david.ramos@example.com", "David", "Ramos", "999.000.111-29",
                                                        "(62) 99990-0189",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "4",
                                                        "GO", "PUC-GO" },
                                        { "90", "1979-10-31", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "eliane.rocha@example.com", "Eliane", "Rocha", "000.111.222-30",
                                                        "(62) 90001-1290",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "5",
                                                        "GO", "PUC-GO" },
                                        { "91", "1998-02-06", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "flavio.sales@example.com", "Flávio", "Sales", "101.202.303-41",
                                                        "(62) 91012-0391",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "7",
                                                        "GO", "PUC-GO" },
                                        { "92", "1981-09-14", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "marta.teixeira@example.com", "Marta", "Teixeira",
                                                        "202.303.404-52", "(62) 92023-0492",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "6",
                                                        "GO", "PUC-GO" },
                                        { "93", "1992-03-03", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "alan.vieira@example.com", "Alan", "Vieira", "303.404.505-63",
                                                        "(62) 93034-0593",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "4",
                                                        "GO", "PUC-GO" },
                                        { "94", "1976-11-22", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "sonia.xavier@example.com", "Sônia", "Xavier", "404.505.606-74",
                                                        "(62) 94045-0694",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "5",
                                                        "GO", "PUC-GO" },
                                        { "95", "1997-08-16", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "cesar.antunes@example.com", "César", "Antunes",
                                                        "505.606.707-85", "(62) 95056-0795",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "7",
                                                        "GO", "PUC-GO" },
                                        { "96", "1984-01-27", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "luciana.borges@example.com", "Luciana", "Borges",
                                                        "606.707.808-96", "(62) 96067-0896",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "6",
                                                        "GO", "PUC-GO" },
                                        { "97", "1990-06-07", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "fabio.camargo@example.com", "Fábio", "Camargo",
                                                        "707.808.909-07", "(62) 97078-0997",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "4",
                                                        "GO", "PUC-GO" },
                                        { "98", "1972-09-19", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "cristina.cardoso@example.com", "Cristina", "Cardoso",
                                                        "808.909.010-18", "(62) 98089-1098",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "5",
                                                        "GO", "PUC-GO" },
                                        { "99", "1989-04-30", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "marcio.cunha@example.com", "Márcio", "Cunha", "909.010.121-29",
                                                        "(62) 99090-1299",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "7",
                                                        "GO", "PUC-GO" },
                                        { "100", "1977-07-11", "Goiânia", "Brasil",
                                                        "Analise e Desenvolvimento de Sistemas",
                                                        "denise.dantas@example.com", "Denise", "Dantas",
                                                        "010.121.232-30", "(62) 90101-2300",
                                                        "https://example.com/imagem_perfil/default.png", "MENTOR", "6",
                                                        "GO", "PUC-GO" }
                        };

                        for (String[] data : mentorData) {
                                long sqlId = Long.parseLong(data[0]);
                                User user = User.builder()
                                                .firstName(data[6])
                                                .lastName(data[7])
                                                .email(data[5])
                                                .cpf(CPFUtils.removerFormatacao(data[8]))
                                                .phone(data[9].replaceAll("[^0-9]", ""))
                                                .password(passwordEncoder
                                                                .encode("Password00"))
                                                .role(Role.valueOf(data[11]))
                                                .birthday(data[1])
                                                .city(data[2])
                                                .state(data[13])
                                                .country(data[3])
                                                .profileImg(data[10])
                                                .courseName(data[4])
                                                .semester(data[12])
                                                .university(data[14])
                                                .build();
                                usersToCreate.add(user);
                        }

                        List<User> savedUsers = userRepository.saveAll(usersToCreate);

                        // Populate the map after saving, so we have the actual generated IDs
                        // We need to match them back to SQL IDs. Assuming order is preserved OR emails
                        // are unique.
                        // Let's use email to map back to SQL ID for robustness.
                        Map<String, Long> emailToSqlIdMap = new HashMap<>();
                        for (String[] data : userData) {
                                emailToSqlIdMap.put(data[5], Long.parseLong(data[0]));
                        }
                        for (String[] data : mentorData) {
                                emailToSqlIdMap.put(data[5], Long.parseLong(data[0]));
                        }

                        for (User savedUser : savedUsers) {
                                Long sqlId = emailToSqlIdMap.get(savedUser.getEmail());
                                if (sqlId != null) {
                                        sqlUserIdToUserObjectMap.put(sqlId, savedUser);
                                } else {
                                        log.warn("Could not map saved user {} back to an SQL ID.",
                                                        savedUser.getEmail());
                                }
                        }
                        sqlUserIdToUserObjectMap.put(0L, savedAdmin); // Assuming admin SQL ID 0 for internal reference
                                                                      // if needed

                        log.info(">>> {} usuários (USER/MENTOR) criados com sucesso a partir dos dados SQL!",
                                        usersToCreate.size());

                } else {
                        log.info(">>> Base de dados já contém usuários. Carregando existentes para mapeamento...");
                        // Load existing users into the map if needed for subsequent steps in a re-run
                        // scenario
                        // This part is tricky if we don't have original SQL IDs stored in the DB
                        // For a clean init, this 'else' block for users might not populate the map
                        // perfectly
                        // without a way to link existing DB users to their original SQL IDs.
                        // Simplest is to assume this method only runs effectively when DB is empty.
                        // If re-running, it's better to clear related tables or handle updates.
                        // For now, if not empty, we'll fetch all and try to map by email if we had the
                        // SQL data again.
                        // But since we checked count == 0, this else branch for users is for future
                        // thought.
                        // For this refactor, we assume it runs on an empty user table or the existing
                        // users
                        // are not meant to be mapped to the new SQL IDs.
                        List<User> existingUsers = userRepository.findAll();
                        for (User user : existingUsers) {
                                // Attempt to find a matching SQL ID - this is non-trivial without original IDs
                                // For now, we'll primarily rely on the map populated during initial creation.
                                // If this method is re-run, ensure previous maps are cleared or updated.
                                // A robust way would be to add a 'sourceSqlId' column to User entity
                                // temporarily
                                // or match by a truly unique business key if email isn't it.
                        }
                }
        }

        private void initializeCourseAreasAndDisciplines() {
                if (courseAreaRepository.count() == 0) {
                        log.info(">>> Base de dados de CourseArea vazia. Criando áreas de curso iniciais...");
                        List<CourseArea> courseAreasToCreate = Arrays.asList(
                                        CourseArea.builder().course("Análise e Desenvolvimento de Sistemas")
                                                        .area("BÁSICO I").build(),
                                        CourseArea.builder().course("Análise e Desenvolvimento de Sistemas")
                                                        .area("BÁSICO II").build(),
                                        CourseArea.builder().course("Análise e Desenvolvimento de Sistemas")
                                                        .area("IA e DEVOPS").build(),
                                        CourseArea.builder().course("Análise e Desenvolvimento de Sistemas").area("WEB")
                                                        .build(),
                                        CourseArea.builder().course("Análise e Desenvolvimento de Sistemas")
                                                        .area("DISPOSITIVOS MÓVEIS").build(),
                                        CourseArea.builder().course("Análise e Desenvolvimento de Sistemas")
                                                        .area("INTEGRADOR").build(),
                                        CourseArea.builder().course("Análise e Desenvolvimento de Sistemas")
                                                        .area("OPTATIVAS").build());
                        courseAreaRepository.saveAll(courseAreasToCreate);
                        log.info(">>> {} áreas de curso iniciais criadas com sucesso!", courseAreasToCreate.size());

                        List<CourseArea> savedCourseAreas = courseAreaRepository.findAll();
                        if (savedCourseAreas.isEmpty() || savedCourseAreas.size() < 7) {
                                log.error(">>> Erro ao buscar áreas de curso salvas. Não é possível criar disciplinas.");
                                return;
                        }

                        if (disciplineRepository.count() == 0) {
                                log.info(">>> Base de dados de Discipline vazia. Criando disciplinas iniciais...");
                                List<Discipline> disciplinesToCreate = Arrays.asList(
                                                Discipline.builder().disciplineName("ALGORITMOS").description(
                                                                "Introdução à lógica de programação e construção de algoritmos.")
                                                                .courseArea(savedCourseAreas.get(0)).build(), // SQL ID
                                                                                                              // 1
                                                Discipline.builder().disciplineName("FUNDAMENTOS DE COMPUTAÇÃO I")
                                                                .description("Conceitos básicos sobre hardware, software e sistemas computacionais.")
                                                                .courseArea(savedCourseAreas.get(0)).build(), // SQL ID
                                                                                                              // 2
                                                Discipline.builder().disciplineName("LABORATÓRIO DE PROGRAMAÇÃO")
                                                                .description("Prática de programação e desenvolvimento de software inicial.")
                                                                .courseArea(savedCourseAreas.get(0)).build(), // SQL ID
                                                                                                              // 3
                                                Discipline.builder().disciplineName("ENGENHARIA DE SOFTWARE")
                                                                .description("Introdução aos princípios e práticas de engenharia de software.")
                                                                .courseArea(savedCourseAreas.get(0)).build(), // SQL ID
                                                                                                              // 4
                                                Discipline.builder().disciplineName(
                                                                "TEOLOGIA, CIENCIAS EXATAS E TECNOLÓGICAS")
                                                                .description("Relação entre teologia e o campo da ciência e tecnologia.")
                                                                .courseArea(savedCourseAreas.get(0)).build(), // SQL ID
                                                                                                              // 5
                                                Discipline.builder().disciplineName("ENGENHARIA DE REQUISITOS")
                                                                .description("Técnicas para levantamento, análise e especificação de requisitos de software.")
                                                                .courseArea(savedCourseAreas.get(1)).build(), // SQL ID
                                                                                                              // 6
                                                Discipline.builder().disciplineName(
                                                                "FUNDAMENTOS DE PROGRAMAÇÃO ORIENTADA A OBJETO")
                                                                .description("Conceitos e prática da programação orientada a objetos.")
                                                                .courseArea(savedCourseAreas.get(1)).build(), // SQL ID
                                                                                                              // 7
                                                Discipline.builder().disciplineName(
                                                                "FUNDAMENTOS DE SISTEMAS DE COMPUTAÇÃO-(EaD)")
                                                                .description("Aprofundamento em sistemas computacionais (modalidade EaD).")
                                                                .courseArea(savedCourseAreas.get(1)).build(), // SQL ID
                                                                                                              // 8
                                                Discipline.builder().disciplineName(
                                                                "INTRODUCAO A ESTATISTICA PARA INTELIGENCIA ARTIFICIAL")
                                                                .description("Conceitos estatísticos aplicados à inteligência artificial.")
                                                                .courseArea(savedCourseAreas.get(1)).build(), // SQL ID
                                                                                                              // 9
                                                Discipline.builder().disciplineName("PROJETO DE BANCO DE DADOS")
                                                                .description("Modelagem e implementação de bancos de dados relacionais.")
                                                                .courseArea(savedCourseAreas.get(1)).build(), // SQL ID
                                                                                                              // 10
                                                Discipline.builder().disciplineName("SEGURANÇA DA INFORMAÇÃO-(EaD)")
                                                                .description("Princípios e práticas de segurança da informação (modalidade EaD).")
                                                                .courseArea(savedCourseAreas.get(2)).build(), // SQL ID
                                                                                                              // 11
                                                Discipline.builder().disciplineName(
                                                                "PROCESSOS DE SOFTWARE E GERÊNCIA DE CONFIGURAÇÃO COM DEVOPS")
                                                                .description("Metodologias ágeis, CI/CD e práticas DevOps.")
                                                                .courseArea(savedCourseAreas.get(2)).build(), // SQL ID
                                                                                                              // 12
                                                Discipline.builder()
                                                                .disciplineName("ESTRUTURA DE DADOS ORIENTADA A OBJETO")
                                                                .description("Implementação de estruturas de dados usando orientação a objetos.")
                                                                .courseArea(savedCourseAreas.get(2)).build(), // SQL ID
                                                                                                              // 13
                                                Discipline.builder().disciplineName("INTELIGENCIA ARTIFICIAL APLICADA")
                                                                .description("Aplicações práticas de técnicas de inteligência artificial.")
                                                                .courseArea(savedCourseAreas.get(2)).build(), // SQL ID
                                                                                                              // 14
                                                Discipline.builder().disciplineName("DESENVOLVIMENTO DE SOFTWARE WEB")
                                                                .description("Criação de aplicações web front-end e back-end.")
                                                                .courseArea(savedCourseAreas.get(3)).build(), // SQL ID
                                                                                                              // 15
                                                Discipline.builder()
                                                                .disciplineName("MENSAGERIA E STREAMS EM APLICACOES")
                                                                .description("Uso de sistemas de mensageria e processamento de streams.")
                                                                .courseArea(savedCourseAreas.get(3)).build(), // SQL ID
                                                                                                              // 16
                                                Discipline.builder().disciplineName("DESIGN DE SOFTWARE").description(
                                                                "Padrões de projeto e princípios de design de software.")
                                                                .courseArea(savedCourseAreas.get(3)).build(), // SQL ID
                                                                                                              // 17
                                                Discipline.builder().disciplineName(
                                                                "GERÊNCIA DE QUALIDADE DE SOFTWARE (EaD)")
                                                                .description("Processos e técnicas para garantir a qualidade de software (modalidade EaD).")
                                                                .courseArea(savedCourseAreas.get(3)).build(), // SQL ID
                                                                                                              // 18
                                                Discipline.builder()
                                                                .disciplineName("MODELAGEM DE INTERFACES DE USUÁRIO")
                                                                .description("Princípios de design e prototipagem de interfaces de usuário (UI/UX).")
                                                                .courseArea(savedCourseAreas.get(3)).build(), // SQL ID
                                                                                                              // 19
                                                Discipline.builder().disciplineName(
                                                                "FERRAMENTAS VISUAIS DE DESENVOLVIMENTO DE SOFTWARE")
                                                                .description("Uso de ferramentas RAD e de baixo código.")
                                                                .courseArea(savedCourseAreas.get(4)).build(), // SQL ID
                                                                                                              // 20
                                                Discipline.builder().disciplineName(
                                                                "DESENVOLVIMENTO DE APLICATIVOS P/DISPOSITIVOS MÓVEIS")
                                                                .description("Criação de aplicativos para plataformas móveis (Android/iOS).")
                                                                .courseArea(savedCourseAreas.get(4)).build(), // SQL ID
                                                                                                              // 21
                                                Discipline.builder().disciplineName(
                                                                "GOVERNANÇA EM TECNOLOGIA DA INFORMAÇÃO-(EaD)")
                                                                .description("Práticas de gestão e governança de TI (modalidade EaD).")
                                                                .courseArea(savedCourseAreas.get(4)).build(), // SQL ID
                                                                                                              // 22
                                                Discipline.builder().disciplineName(
                                                                "PROGRAMAÇÃO ORIENTADA A OBJETOS COM BANCO DE DADOS")
                                                                .description("Integração de aplicações OO com bancos de dados.")
                                                                .courseArea(savedCourseAreas.get(4)).build(), // SQL ID
                                                                                                              // 23
                                                Discipline.builder().disciplineName(
                                                                "GERÊNCIA DE PROJETOS DE SISTEMAS-(EaD)")
                                                                .description("Planejamento, execução e controle de projetos de software (modalidade EaD).")
                                                                .courseArea(savedCourseAreas.get(4)).build(), // SQL ID
                                                                                                              // 24
                                                Discipline.builder().disciplineName("PROJETO INTEGRADOR").description(
                                                                "Desenvolvimento de um projeto aplicando conhecimentos do curso.")
                                                                .courseArea(savedCourseAreas.get(5)).build(), // SQL ID
                                                                                                              // 25
                                                Discipline.builder().disciplineName(
                                                                "INTRODUCAO A BIG DATA E CIENCIA DE DADOS-EAD")
                                                                .description("Conceitos fundamentais de Big Data e Ciência de Dados (modalidade EaD).")
                                                                .courseArea(savedCourseAreas.get(5)).build(), // SQL ID
                                                                                                              // 26
                                                Discipline.builder().disciplineName("INTERNET DAS COISAS").description(
                                                                "Princípios e tecnologias de Internet of Things (IoT).")
                                                                .courseArea(savedCourseAreas.get(5)).build(), // SQL ID
                                                                                                              // 27
                                                Discipline.builder().disciplineName(
                                                                "NEGÓCIOS em TECNOLOGIA DA INFORMAÇÃO-(EAD)")
                                                                .description("Aspectos de negócios e empreendedorismo em TI (modalidade EaD).")
                                                                .courseArea(savedCourseAreas.get(5)).build(), // SQL ID
                                                                                                              // 28
                                                Discipline.builder().disciplineName("OPTATIVA I").description(
                                                                "Disciplina optativa a ser definida conforme escolha do aluno.")
                                                                .courseArea(savedCourseAreas.get(5)).build(), // SQL ID
                                                                                                              // 29 //
                                                                                                              // Corrected
                                                                                                              // index
                                                Discipline.builder().disciplineName("LIBRAS INSTRUMENTAL").description(
                                                                "Língua Brasileira de Sinais com foco instrumental.")
                                                                .courseArea(savedCourseAreas.get(6)).build(), // SQL ID
                                                                                                              // 30
                                                Discipline.builder().disciplineName("INTERPRETAÇÃO DE TEXTO")
                                                                .description("Técnicas de leitura e interpretação textual.")
                                                                .courseArea(savedCourseAreas.get(6)).build(), // SQL ID
                                                                                                              // 31
                                                Discipline.builder().disciplineName(
                                                                "PARADIGMAS DE LINGUAGEM DE PROGRAMAÇÃO")
                                                                .description("Estudo de diferentes paradigmas de programação.")
                                                                .courseArea(savedCourseAreas.get(6)).build(), // SQL ID
                                                                                                              // 32
                                                Discipline.builder().disciplineName("PROBABILIDADE E ESTATÍSTICA")
                                                                .description("Conceitos avançados de probabilidade e estatística.")
                                                                .courseArea(savedCourseAreas.get(6)).build(), // SQL ID
                                                                                                              // 33
                                                Discipline.builder()
                                                                .disciplineName("PRÁTICAS DE DESENVOLVIMENTO DE JOGOS")
                                                                .description("Técnicas e ferramentas para desenvolvimento de jogos.")
                                                                .courseArea(savedCourseAreas.get(6)).build(), // SQL ID
                                                                                                              // 34
                                                Discipline.builder().disciplineName("FUNDAMENTOS DE JOGOS")
                                                                .description("Teoria e design de jogos digitais.")
                                                                .courseArea(savedCourseAreas.get(6)).build(), // SQL ID
                                                                                                              // 35
                                                Discipline.builder().disciplineName("BANCO DE DADOS II")
                                                                .description("Tópicos avançados em bancos de dados.")
                                                                .courseArea(savedCourseAreas.get(6)).build(), // SQL ID
                                                                                                              // 36
                                                Discipline.builder().disciplineName("REDES DE COMPUTADORES I")
                                                                .description("Fundamentos de redes de computadores e protocolos.")
                                                                .courseArea(savedCourseAreas.get(6)).build() // SQL ID
                                                                                                             // 37
                                );
                                disciplineRepository.saveAll(disciplinesToCreate);
                                log.info(">>> {} disciplinas iniciais criadas com sucesso!",
                                                disciplinesToCreate.size());
                        } else {
                                log.info(">>> Base de dados já contém disciplinas. Nenhuma ação necessária para disciplinas.");
                        }
                } else {
                        log.info(">>> Base de dados já contém áreas de curso. Nenhuma ação necessária para áreas de curso e disciplinas.");
                }
        }

        private void mapSqlDisciplineIdsToEntities() {
                if (sqlDisciplineIdToDisciplineObjectMap.isEmpty() && disciplineRepository.count() > 0) {
                        log.info(">>> Mapeando IDs SQL de disciplinas para entidades persistidas...");
                        List<Discipline> allDisciplines = disciplineRepository.findAll();
                        // This mapping assumes the order of creation in
                        // initializeCourseAreasAndDisciplines
                        // matches the SQL IDs 1-37. This is fragile.
                        // A better way would be to match by name if names are unique and provided in
                        // SQL.
                        // For now, using the order as a simplification.
                        if (allDisciplines.size() >= 37) {
                                for (int i = 0; i < 37; i++) {
                                        sqlDisciplineIdToDisciplineObjectMap.put((long) (i + 1), allDisciplines.get(i));
                                }
                                log.info(">>> {} disciplinas mapeadas.", sqlDisciplineIdToDisciplineObjectMap.size());
                        } else {
                                log.warn(">>> Não foi possível mapear IDs de disciplinas SQL: número de disciplinas no BD ({}) é menor que 37.",
                                                allDisciplines.size());
                        }
                }
        }

        private void initializeMentorAvailabilitiesFromSqlData() {
                if (mentorAvailabilityRepository.count() == 0 && !sqlUserIdToUserObjectMap.isEmpty()
                                && !sqlDisciplineIdToDisciplineObjectMap.isEmpty()) {
                        log.info(">>> Criando disponibilidades de mentores a partir dos dados SQL...");
                        List<MentorAvailability> availabilitiesToCreate = new ArrayList<>();

                        // Mentor Availability Data (120 entries)
                        // {endTime, isAvailable, startTime, tutoringClassType, disciplineId,
                        // mentorSqlId, dayOfWeek}
                        Object[][] availabilityData = {
                                        { "09:00:00", true, "08:00:00", "ONLINE", 1L, 71L, "SEGUNDA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 2L, 71L, "SEGUNDA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "ONLINE", 3L, 71L, "TERCA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "PRESENCIAL", 4L, 71L, "TERCA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 5L, 72L, "SEGUNDA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 6L, 72L, "SEGUNDA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 7L, 72L, "QUARTA_FEIRA" },
                                        { "18:00:00", true, "17:00:00", "ONLINE", 8L, 72L, "QUARTA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 9L, 73L, "TERCA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 10L, 73L, "TERCA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 11L, 73L, "QUINTA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 12L, 73L, "QUINTA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 13L, 74L, "QUARTA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 14L, 74L, "QUARTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 15L, 74L, "SEXTA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 16L, 74L, "SEXTA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 17L, 75L, "QUINTA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 18L, 75L, "QUINTA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 19L, 75L, "SEGUNDA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 20L, 75L, "SEGUNDA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 21L, 76L, "SEXTA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 22L, 76L, "SEXTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 23L, 76L, "TERCA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 24L, 76L, "TERCA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 25L, 77L, "SEGUNDA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 26L, 77L, "QUARTA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 27L, 77L, "SEXTA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 28L, 77L, "SEXTA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 29L, 78L, "TERCA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 30L, 78L, "QUINTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 31L, 78L, "SEGUNDA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 32L, 78L, "QUARTA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 33L, 79L, "QUARTA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 34L, 79L, "SEXTA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 35L, 79L, "SEGUNDA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 36L, 79L, "TERCA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 37L, 80L, "QUINTA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 1L, 80L, "SEXTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 2L, 80L, "SEGUNDA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 3L, 80L, "TERCA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 4L, 81L, "SEGUNDA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 5L, 81L, "TERCA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 6L, 81L, "QUARTA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 7L, 81L, "QUINTA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 8L, 82L, "SEXTA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 9L, 82L, "SEGUNDA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 10L, 82L, "TERCA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 11L, 82L, "QUARTA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 12L, 83L, "QUINTA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 13L, 83L, "SEXTA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 14L, 83L, "SEGUNDA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 15L, 83L, "TERCA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 16L, 84L, "QUARTA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 17L, 84L, "QUINTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 18L, 84L, "SEXTA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 19L, 84L, "SEGUNDA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 20L, 85L, "TERCA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 21L, 85L, "QUARTA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 22L, 85L, "QUINTA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 23L, 85L, "SEXTA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 24L, 86L, "SEGUNDA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 25L, 86L, "TERCA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 26L, 86L, "QUARTA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 27L, 86L, "QUINTA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 28L, 87L, "SEXTA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 29L, 87L, "SEGUNDA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 30L, 87L, "TERCA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 31L, 87L, "QUARTA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 32L, 88L, "QUINTA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 33L, 88L, "SEXTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 34L, 88L, "SEGUNDA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 35L, 88L, "TERCA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 36L, 89L, "QUARTA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 37L, 89L, "QUINTA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 1L, 89L, "SEXTA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 2L, 89L, "SEGUNDA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 3L, 90L, "TERCA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 4L, 90L, "QUARTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 5L, 90L, "QUINTA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 6L, 90L, "SEXTA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 7L, 91L, "SEGUNDA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 8L, 91L, "TERCA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 9L, 91L, "QUARTA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 10L, 91L, "QUINTA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 11L, 92L, "SEXTA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 12L, 92L, "SEGUNDA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 13L, 92L, "TERCA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 14L, 92L, "QUARTA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 15L, 93L, "QUINTA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 16L, 93L, "SEXTA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 17L, 93L, "SEGUNDA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 18L, 93L, "TERCA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 19L, 94L, "QUARTA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 20L, 94L, "QUINTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 21L, 94L, "SEXTA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 22L, 94L, "SEGUNDA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 23L, 95L, "TERCA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 24L, 95L, "QUARTA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 25L, 95L, "QUINTA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 26L, 95L, "SEXTA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 27L, 96L, "SEGUNDA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 28L, 96L, "TERCA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 29L, 96L, "QUARTA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 30L, 96L, "QUINTA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 31L, 97L, "SEXTA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 32L, 97L, "SEGUNDA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 33L, 97L, "TERCA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 34L, 97L, "QUARTA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 35L, 98L, "QUINTA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 36L, 98L, "SEXTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 37L, 98L, "SEGUNDA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 1L, 98L, "TERCA_FEIRA" },
                                        { "09:00:00", true, "08:00:00", "ONLINE", 2L, 99L, "QUARTA_FEIRA" },
                                        { "11:00:00", true, "10:00:00", "PRESENCIAL", 3L, 99L, "QUINTA_FEIRA" },
                                        { "14:00:00", true, "13:00:00", "ONLINE", 4L, 99L, "SEXTA_FEIRA" },
                                        { "16:00:00", true, "15:00:00", "PRESENCIAL", 5L, 99L, "SEGUNDA_FEIRA" },
                                        { "10:00:00", true, "09:00:00", "PRESENCIAL", 6L, 100L, "TERCA_FEIRA" },
                                        { "12:00:00", true, "11:00:00", "ONLINE", 7L, 100L, "QUARTA_FEIRA" },
                                        { "15:00:00", true, "14:00:00", "PRESENCIAL", 8L, 100L, "QUINTA_FEIRA" },
                                        { "17:00:00", true, "16:00:00", "ONLINE", 9L, 100L, "SEXTA_FEIRA" }
                        };

                        for (Object[] data : availabilityData) {
                                User mentor = sqlUserIdToUserObjectMap.get((Long) data[5]);
                                Discipline discipline = sqlDisciplineIdToDisciplineObjectMap.get((Long) data[4]);

                                if (mentor == null) {
                                        log.warn("Mentor com SQL ID {} não encontrado no mapa. Pulando disponibilidade.",
                                                        data[5]);
                                        continue;
                                }
                                if (discipline == null) {
                                        log.warn("Disciplina com SQL ID {} não encontrada no mapa. Pulando disponibilidade.",
                                                        data[4]);
                                        continue;
                                }
                                if (mentor.getRole() != Role.MENTOR) {
                                        mentor.setRole(Role.MENTOR);
                                        userRepository.save(mentor); // Promote to mentor
                                }

                                MentorAvailability availability = MentorAvailability.builder()
                                                .user(mentor)
                                                .discipline(discipline)
                                                .dayOfWeek(DayWeek.valueOf((String) data[6]))
                                                .startTime(LocalTime.parse((String) data[2]))
                                                .endTime(LocalTime.parse((String) data[0]))
                                                .tutoringClassType(TutoringClassType.valueOf((String) data[3]))
                                                .isAvailable((Boolean) data[1])
                                                .build();
                                availabilitiesToCreate.add(availability);
                        }
                        mentorAvailabilityRepository.saveAll(availabilitiesToCreate);
                        log.info(">>> {} disponibilidades de mentores criadas com sucesso a partir dos dados SQL.",
                                        availabilitiesToCreate.size());
                } else {
                        log.info(">>> Disponibilidades de mentores já existem ou mapas de usuário/disciplina estão vazios. Nenhuma ação necessária.");
                }
        }

        private void initializeTutoringsAndParticipantsAndRatingsFromSqlData() {
                if (tutoringRepository.count() == 0 && !sqlUserIdToUserObjectMap.isEmpty()
                                && !sqlDisciplineIdToDisciplineObjectMap.isEmpty()) {
                        log.info(">>> Criando tutorias, participantes e avaliações a partir dos dados SQL...");

                        // Tutoring Data
                        // {endTime, isChatEnable, isMentorPostingOnly, maxParticipants, startTime,
                        // tutoringDateStr,
                        // disciplineSqlId, tutoringSqlId, mentorSqlId, linkVideo, local, statusStr,
                        // classTypeStr}
                        Object[][] tutoringData = {
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-19", 1L, 1L, 71L,
                                                        "https://example.com/recording/tutoring1.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-12", 2L, 2L, 71L, null,
                                                        "PUC-GO Bloco D Sala 102", "CONCLUIDA", "PRESENCIAL" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-06", 3L, 3L, 71L,
                                                        "https://example.com/recording/tutoring3.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-19", 5L, 4L, 72L, null,
                                                        "PUC-GO Bloco D Sala 104", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-12", 6L, 5L, 72L,
                                                        "https://example.com/recording/tutoring5.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "16:00:00", true, false, 10, "15:00:00", "2025-05-07", 7L, 6L, 72L, null,
                                                        "PUC-GO Bloco D Sala 106", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-20", 9L, 7L, 73L,
                                                        "https://example.com/recording/tutoring7.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-13", 10L, 8L, 73L, null,
                                                        "PUC-GO Bloco D Sala 108", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-08", 11L, 9L, 73L,
                                                        "https://example.com/recording/tutoring9.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-21", 13L, 10L, 74L, null,
                                                        "PUC-GO Bloco D Sala 100", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-14", 14L, 11L, 74L,
                                                        "https://example.com/recording/tutoring11.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-09", 15L, 12L, 74L, null,
                                                        "PUC-GO Bloco D Sala 102", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-22", 17L, 13L, 75L,
                                                        "https://example.com/recording/tutoring13.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-15", 18L, 14L, 75L, null,
                                                        "PUC-GO Bloco D Sala 104", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-05", 19L, 15L, 75L,
                                                        "https://example.com/recording/tutoring15.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-23", 21L, 16L, 76L, null,
                                                        "PUC-GO Bloco D Sala 106", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-16", 22L, 17L, 76L,
                                                        "https://example.com/recording/tutoring17.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-06", 23L, 18L, 76L, null,
                                                        "PUC-GO Bloco D Sala 108", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-19", 25L, 19L, 77L,
                                                        "https://example.com/recording/tutoring19.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-14", 26L, 20L, 77L, null,
                                                        "PUC-GO Bloco D Sala 100", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-09", 27L, 21L, 77L,
                                                        "https://example.com/recording/tutoring21.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-20", 29L, 22L, 78L, null,
                                                        "PUC-GO Bloco D Sala 102", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-15", 30L, 23L, 78L,
                                                        "https://example.com/recording/tutoring23.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-05", 31L, 24L, 78L, null,
                                                        "PUC-GO Bloco D Sala 104", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-21", 33L, 25L, 79L,
                                                        "https://example.com/recording/tutoring25.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-16", 34L, 26L, 79L, null,
                                                        "PUC-GO Bloco D Sala 106", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-05", 35L, 27L, 79L,
                                                        "https://example.com/recording/tutoring27.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-22", 37L, 28L, 80L, null,
                                                        "PUC-GO Bloco D Sala 108", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-16", 1L, 29L, 80L,
                                                        "https://example.com/recording/tutoring29.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-05", 2L, 30L, 80L, null,
                                                        "PUC-GO Bloco D Sala 100", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-19", 4L, 31L, 81L,
                                                        "https://example.com/recording/tutoring31.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-13", 5L, 32L, 81L, null,
                                                        "PUC-GO Bloco D Sala 102", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-07", 6L, 33L, 81L,
                                                        "https://example.com/recording/tutoring33.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-23", 8L, 34L, 82L, null,
                                                        "PUC-GO Bloco D Sala 104", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-12", 9L, 35L, 82L,
                                                        "https://example.com/recording/tutoring35.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-06", 10L, 36L, 82L, null,
                                                        "PUC-GO Bloco D Sala 106", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-22", 12L, 37L, 83L,
                                                        "https://example.com/recording/tutoring37.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-16", 13L, 38L, 83L, null,
                                                        "PUC-GO Bloco D Sala 108", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-05", 14L, 39L, 83L,
                                                        "https://example.com/recording/tutoring39.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-21", 16L, 40L, 84L, null,
                                                        "PUC-GO Bloco D Sala 100", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-15", 17L, 41L, 84L,
                                                        "https://example.com/recording/tutoring41.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-09", 18L, 42L, 84L, null,
                                                        "PUC-GO Bloco D Sala 102", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-20", 20L, 43L, 85L,
                                                        "https://example.com/recording/tutoring43.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-14", 21L, 44L, 85L, null,
                                                        "PUC-GO Bloco D Sala 104", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-08", 22L, 45L, 85L,
                                                        "https://example.com/recording/tutoring45.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-19", 24L, 46L, 86L, null,
                                                        "PUC-GO Bloco D Sala 106", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-13", 25L, 47L, 86L,
                                                        "https://example.com/recording/tutoring47.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-07", 26L, 48L, 86L, null,
                                                        "PUC-GO Bloco D Sala 108", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-23", 28L, 49L, 87L,
                                                        "https://example.com/recording/tutoring49.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-12", 29L, 50L, 87L, null,
                                                        "PUC-GO Bloco D Sala 100", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-06", 30L, 51L, 87L,
                                                        "https://example.com/recording/tutoring51.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-22", 32L, 52L, 88L, null,
                                                        "PUC-GO Bloco D Sala 102", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-16", 33L, 53L, 88L,
                                                        "https://example.com/recording/tutoring53.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-05", 34L, 54L, 88L, null,
                                                        "PUC-GO Bloco D Sala 104", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-21", 36L, 55L, 89L,
                                                        "https://example.com/recording/tutoring55.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-15", 37L, 56L, 89L, null,
                                                        "PUC-GO Bloco D Sala 106", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-09", 1L, 57L, 89L,
                                                        "https://example.com/recording/tutoring57.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-20", 3L, 58L, 90L, null,
                                                        "PUC-GO Bloco D Sala 108", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-14", 4L, 59L, 90L,
                                                        "https://example.com/recording/tutoring59.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-08", 5L, 60L, 90L, null,
                                                        "PUC-GO Bloco D Sala 100", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-19", 7L, 61L, 91L,
                                                        "https://example.com/recording/tutoring61.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-13", 8L, 62L, 91L, null,
                                                        "PUC-GO Bloco D Sala 102", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-07", 9L, 63L, 91L,
                                                        "https://example.com/recording/tutoring63.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-23", 11L, 64L, 92L, null,
                                                        "PUC-GO Bloco D Sala 104", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-12", 12L, 65L, 92L,
                                                        "https://example.com/recording/tutoring65.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-06", 13L, 66L, 92L, null,
                                                        "PUC-GO Bloco D Sala 106", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-22", 15L, 67L, 93L,
                                                        "https://example.com/recording/tutoring67.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-16", 16L, 68L, 93L, null,
                                                        "PUC-GO Bloco D Sala 108", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-05", 17L, 69L, 93L,
                                                        "https://example.com/recording/tutoring69.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-21", 19L, 70L, 94L, null,
                                                        "PUC-GO Bloco D Sala 100", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-15", 20L, 71L, 94L,
                                                        "https://example.com/recording/tutoring71.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-09", 21L, 72L, 94L, null,
                                                        "PUC-GO Bloco D Sala 102", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-20", 23L, 73L, 95L,
                                                        "https://example.com/recording/tutoring73.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-14", 24L, 74L, 95L, null,
                                                        "PUC-GO Bloco D Sala 104", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-08", 25L, 75L, 95L,
                                                        "https://example.com/recording/tutoring75.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-19", 27L, 76L, 96L, null,
                                                        "PUC-GO Bloco D Sala 106", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-13", 28L, 77L, 96L,
                                                        "https://example.com/recording/tutoring77.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-07", 29L, 78L, 96L, null,
                                                        "PUC-GO Bloco D Sala 108", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-23", 31L, 79L, 97L,
                                                        "https://example.com/recording/tutoring79.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-12", 32L, 80L, 97L, null,
                                                        "PUC-GO Bloco D Sala 100", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-06", 33L, 81L, 97L,
                                                        "https://example.com/recording/tutoring81.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-22", 35L, 82L, 98L, null,
                                                        "PUC-GO Bloco D Sala 102", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-16", 36L, 83L, 98L,
                                                        "https://example.com/recording/tutoring83.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-05", 37L, 84L, 98L, null,
                                                        "PUC-GO Bloco D Sala 104", "CONCLUIDA", "PRESENCIAL" },
                                        { "09:00:00", true, false, 10, "08:00:00", "2025-05-21", 2L, 85L, 99L,
                                                        "https://example.com/recording/tutoring85.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "11:00:00", true, false, 10, "10:00:00", "2025-05-15", 3L, 86L, 99L, null,
                                                        "PUC-GO Bloco D Sala 106", "CONCLUIDA", "PRESENCIAL" },
                                        { "14:00:00", true, false, 10, "13:00:00", "2025-05-09", 4L, 87L, 99L,
                                                        "https://example.com/recording/tutoring87.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "10:00:00", true, false, 10, "09:00:00", "2025-05-20", 6L, 88L, 100L, null,
                                                        "PUC-GO Bloco D Sala 108", "CONCLUIDA", "PRESENCIAL" },
                                        { "12:00:00", true, false, 10, "11:00:00", "2025-05-14", 7L, 89L, 100L,
                                                        "https://example.com/recording/tutoring89.mp4", null,
                                                        "CONCLUIDA", "ONLINE" },
                                        { "15:00:00", true, false, 10, "14:00:00", "2025-05-08", 8L, 90L, 100L, null,
                                                        "PUC-GO Bloco D Sala 100", "CONCLUIDA", "PRESENCIAL" }
                        };

                        List<Tutoring> tutoringsToSave = new ArrayList<>();
                        for (Object[] data : tutoringData) {
                                User mentor = sqlUserIdToUserObjectMap.get((Long) data[8]);
                                Discipline discipline = sqlDisciplineIdToDisciplineObjectMap.get((Long) data[6]);
                                Long tutoringSqlId = (Long) data[7];

                                if (mentor == null || discipline == null) {
                                        log.warn("Mentor (SQL ID: {}) ou Disciplina (SQL ID: {}) não encontrado(s) para Tutoria SQL ID {}. Pulando.",
                                                        data[8], data[6], tutoringSqlId);
                                        continue;
                                }

                                Tutoring tutoring = Tutoring.builder()
                                                .mentor(mentor)
                                                .discipline(discipline)
                                                .tutoringClassType(TutoringClassType.valueOf((String) data[12]))
                                                .status(StatusTutoring.valueOf((String) data[11]))
                                                .tutoringDate(LocalDate.parse((String) data[5], SQL_DATE_FORMATTER))
                                                .startTime(LocalTime.parse((String) data[4]))
                                                .endTime(LocalTime.parse((String) data[0]))
                                                .isChatEnable((Boolean) data[1])
                                                .isMentorPostingOnly((Boolean) data[2])
                                                .maxParticipants((Integer) data[3])
                                                .linkVideo((String) data[9])
                                                .local((String) data[10])
                                                .topics(new HashSet<>()) // Participants will be added later
                                                .build();

                                Tutoring savedTutoring = tutoringRepository.save(tutoring);
                                sqlTutoringIdToTutoringObjectMap.put(tutoringSqlId, savedTutoring);
                                tutoringsToSave.add(savedTutoring); // Though already saved, might be useful for batch
                                                                    // operations later
                        }
                        log.info(">>> {} tutorias criadas com sucesso a partir dos dados SQL.", tutoringsToSave.size());

                        // Tutoring Participants Data
                        // {tutoringSqlId, userSqlId, topic}
                        Object[][] participantData = {
                                        { 1L, 1L, "Dúvida sobre laços de repetição em Algoritmos (tutoria 1)" },
                                        { 31L, 1L, "Qual a melhor forma de versionar código em Lab. de Programação? (tutoria 31)" },
                                        { 2L, 2L, "Como funcionam sistemas de numeração em Fund. de Comp. I? (tutoria 2)" },
                                        { 32L, 2L, "Dúvida sobre testes unitários em Lab. de Programação. (tutoria 32)" },
                                        { 3L, 3L, "Melhores práticas para depuração. (tutoria 3)" },
                                        { 33L, 3L, "Como aplicar o Scrum em projetos pequenos? (tutoria 33)" },
                                        { 4L, 4L, "O que são requisitos não funcionais em Eng. de Software? (tutoria 4)" },
                                        { 34L, 4L, "Dúvida sobre Teologia e tecnologia. (tutoria 34)" },
                                        { 5L, 5L, "Como a fé se relaciona com as ciências exatas? (tutoria 5)" },
                                        { 35L, 5L, "Como definir o escopo em Eng. de Requisitos? (tutoria 35)" },
                                        { 6L, 6L, "Técnicas de elicitação de requisitos. (tutoria 6)" },
                                        { 36L, 6L, "Princípios SOLID em POO. (tutoria 36)" },
                                        { 7L, 7L, "Diferença entre classe e objeto. (tutoria 7)" },
                                        { 37L, 7L, "Como funciona a memória cache em Fund. Sist. Comp.? (tutoria 37)" },
                                        { 8L, 8L, "Arquitetura de Von Neumann. (tutoria 8)" },
                                        { 38L, 8L, "O que é um desvio padrão em Estatística para IA? (tutoria 38)" },
                                        { 9L, 9L, "Teste de hipóteses. (tutoria 9)" },
                                        { 39L, 9L, "Como fazer a modelagem MER para um e-commerce? (tutoria 39)" },
                                        { 10L, 10L, "Formas normais em Banco de Dados. (tutoria 10)" },
                                        { 40L, 10L, "O que é criptografia simétrica em Seg. da Informação? (tutoria 40)" },
                                        { 11L, 11L, "Firewalls e VPNs. (tutoria 11)" },
                                        { 41L, 11L, "Como o DevOps melhora o deploy? (tutoria 41)" },
                                        { 12L, 12L, "Integração Contínua (CI). (tutoria 12)" },
                                        { 42L, 12L, "Implementação de listas encadeadas em Estrutura de Dados. (tutoria 42)" },
                                        { 13L, 13L, "Árvores binárias de busca. (tutoria 13)" },
                                        { 43L, 13L, "Redes Neurais em IA Aplicada. (tutoria 43)" },
                                        { 14L, 14L, "Algoritmos de aprendizado supervisionado. (tutoria 14)" },
                                        { 44L, 14L, "Como construir uma API REST com Node.js? (tutoria 44)" },
                                        { 15L, 15L, "Diferenças entre GET e POST no Desenvolvimento Web. (tutoria 15)" },
                                        { 45L, 15L, "Como usar Kafka para mensageria? (tutoria 45)" },
                                        { 16L, 16L, "RabbitMQ vs Kafka. (tutoria 16)" },
                                        { 46L, 16L, "Padrões de projeto em Design de Software. (tutoria 46)" },
                                        { 17L, 17L, "Princípio da Responsabilidade Única. (tutoria 17)" },
                                        { 47L, 17L, "O que é TQM em Gerência de Qualidade? (tutoria 47)" },
                                        { 18L, 18L, "Normas ISO para software. (tutoria 18)" },
                                        { 48L, 18L, "Heurísticas de Nielsen para UI. (tutoria 48)" },
                                        { 19L, 19L, "Prototipação de interfaces. (tutoria 19)" },
                                        { 49L, 19L, "Como usar o Figma para desenvolvimento visual? (tutoria 49)" },
                                        { 20L, 20L, "Low-code vs No-code. (tutoria 20)" },
                                        { 50L, 20L, "Desenvolvimento nativo vs híbrido para mobile. (tutoria 50)" },
                                        { 21L, 21L, "Como publicar um app na Play Store? (tutoria 21)" },
                                        { 51L, 21L, "O que é COBIT em Governança de TI? (tutoria 51)" },
                                        { 22L, 22L, "ITIL vs COBIT. (tutoria 22)" },
                                        { 52L, 22L, "Mapeamento Objeto-Relacional (ORM) com Java. (tutoria 52)" },
                                        { 23L, 23L, "Conexão JDBC. (tutoria 23)" },
                                        { 53L, 23L, "Como elaborar um cronograma em Gerência de Projetos? (tutoria 53)" },
                                        { 24L, 24L, "Gerenciamento de riscos em projetos. (tutoria 24)" },
                                        { 54L, 24L, "Escopo do Projeto Integrador. (tutoria 54)" },
                                        { 25L, 25L, "Como definir entregáveis do projeto? (tutoria 25)" },
                                        { 55L, 25L, "O que é Hadoop em Big Data? (tutoria 55)" },
                                        { 26L, 26L, "Spark vs MapReduce. (tutoria 26)" },
                                        { 56L, 26L, "Protocolos MQTT em IoT. (tutoria 56)" },
                                        { 27L, 27L, "Segurança em dispositivos IoT. (tutoria 27)" },
                                        { 57L, 27L, "Modelos de negócio para SaaS em Negócios em TI. (tutoria 57)" },
                                        { 28L, 28L, "Como precificar um produto de TI? (tutoria 28)" },
                                        { 58L, 28L, "Escolha da Optativa I. (tutoria 58)" },
                                        { 29L, 29L, "Tópico da Optativa I. (tutoria 29)" },
                                        { 59L, 29L, "Sinais básicos em LIBRAS. (tutoria 59)" },
                                        { 30L, 30L, "Cultura surda. (tutoria 30)" },
                                        { 60L, 30L, "Interpretação de texto técnico. (tutoria 60)" },
                                        { 61L, 31L, "Paradigmas funcional vs orientado a objetos. (tutoria 61)" },
                                        { 62L, 32L, "Cálculo de probabilidade. (tutoria 62)" },
                                        { 63L, 33L, "Distribuição Normal em estatística. (tutoria 63)" },
                                        { 64L, 34L, "Game engines populares para desenvolvimento de jogos. (tutoria 64)" },
                                        { 65L, 35L, "História dos videogames em Fundamentos de Jogos. (tutoria 65)" },
                                        { 66L, 36L, "Consultas SQL avançadas em Banco de Dados II. (tutoria 66)" },
                                        { 67L, 37L, "Modelo OSI em Redes de Computadores I. (tutoria 67)" },
                                        { 68L, 38L, "Dúvida sobre Algoritmos genéticos (Disciplina de IA Aplicada). (tutoria 68)" },
                                        { 69L, 39L, "Como usar Bootstrap no Desenvolvimento Web? (tutoria 69)" },
                                        { 70L, 40L, "Streams de vídeo com WebRTC. (tutoria 70)" },
                                        { 71L, 41L, "Padrão MVC em Design de Software. (tutoria 71)" },
                                        { 72L, 42L, "Ferramentas de teste de software. (tutoria 72)" },
                                        { 73L, 43L, "Design responsivo para interfaces. (tutoria 73)" },
                                        { 74L, 44L, "Uso do Power BI para visualização de dados. (tutoria 74)" },
                                        { 75L, 45L, "Gerenciamento de estado no Flutter. (tutoria 75)" },
                                        { 76L, 46L, "Frameworks de Governança de TI. (tutoria 76)" },
                                        { 77L, 47L, "Hibernate vs JPA. (tutoria 77)" },
                                        { 78L, 48L, "Metodologia PMBOK. (tutoria 78)" },
                                        { 79L, 49L, "Apresentação do Projeto Integrador. (tutoria 79)" },
                                        { 80L, 50L, "Ferramentas de ETL para Big Data. (tutoria 80)" },
                                        { 81L, 51L, "Raspberry Pi para projetos IoT. (tutoria 81)" },
                                        { 82L, 52L, "Canvas Business Model em Negócios de TI. (tutoria 82)" },
                                        { 83L, 53L, "Discussão sobre Optativa I. (tutoria 83)" },
                                        { 84L, 54L, "Comunicação em LIBRAS. (tutoria 84)" },
                                        { 85L, 55L, "Coesão e coerência textual. (tutoria 85)" },
                                        { 86L, 56L, "Linguagens de programação funcionais. (tutoria 86)" },
                                        { 87L, 57L, "Teorema Central do Limite. (tutoria 87)" },
                                        { 88L, 58L, "Roteirização em jogos. (tutoria 88)" },
                                        { 89L, 59L, "Gêneros de jogos. (tutoria 89)" },
                                        { 90L, 60L, "Stored Procedures em Banco de Dados II. (tutoria 90)" },
                                        { 1L, 61L, "Tipos de endereçamento IP (Redes I). (tutoria 1 adicional para user 61)" },
                                        { 5L, 62L, "Como a Teologia vê a IA? (tutoria 5)" },
                                        { 10L, 63L, "Backup e restauração de bancos de dados. (tutoria 10)" },
                                        { 15L, 64L, "Segurança em APIs (Desenvolvimento Web). (tutoria 15)" },
                                        { 20L, 65L, "RAD para desenvolvimento rápido. (tutoria 20)" },
                                        { 25L, 66L, "Como apresentar o TCC (Projeto Integrador)? (tutoria 25)" },
                                        { 30L, 67L, "Saudações em LIBRAS. (tutoria 30)" },
                                        { 35L, 68L, "O que é game feel (Fund. Jogos)? (tutoria 35)" },
                                        { 40L, 69L, "Malware e antivírus (Seg. Informação). (tutoria 40)" },
                                        { 45L, 70L, "Conceito de microserviços em Mensageria. (tutoria 45)" }
                        };

                        List<TutoringParticipants> participantsToSave = new ArrayList<>();
                        for (Object[] data : participantData) {
                                Tutoring tutoring = sqlTutoringIdToTutoringObjectMap.get((Long) data[0]);
                                User participantUser = sqlUserIdToUserObjectMap.get((Long) data[1]);

                                if (tutoring == null || participantUser == null) {
                                        log.warn("Tutoria (SQL ID: {}) ou Participante (SQL ID: {}) não encontrado(s). Pulando participação.",
                                                        data[0], data[1]);
                                        continue;
                                }

                                TutoringParticipants participant = TutoringParticipants.builder()
                                                .tutoring(tutoring)
                                                .user(participantUser)
                                                .topic((String) data[2])
                                                .build();
                                participantsToSave.add(participant);
                                tutoring.getTopics().add(participant); // Add to the set in Tutoring entity
                        }
                        tutoringParticipantsRepository.saveAll(participantsToSave);
                        // Re-save tutorings if the @OneToMany relationship is not managed by cascade
                        // from TutoringParticipants
                        // or if changes to Tutoring (like adding to topics set) need to be persisted.
                        // With CascadeType.ALL on Tutoring.topics and TutoringParticipants.tutoring
                        // being the owner,
                        // saving participants should be enough if Tutoring side of relationship is
                        // correctly updated.
                        // However, explicit save of tutoring can ensure consistency if cascades are
                        // complex.
                        // For simplicity, we assume saving participants and having them linked to
                        // tutoring is enough for now.
                        // If using bidirectional @OneToMany and @ManyToOne, ensure both sides are
                        // consistent.
                        // The current Tutoring.topics is `cascade = CascadeType.ALL, orphanRemoval =
                        // true`,
                        // and TutoringParticipants.tutoring is `@ManyToOne... @JoinColumn(name =
                        // "tutoring_id")`.
                        // Adding to `tutoring.getTopics()` and then saving `tutoring` *might* be an
                        // alternative,
                        // but saving participants which hold the FK to tutoring is generally more
                        // direct.
                        log.info(">>> {} participações em tutorias criadas com sucesso a partir dos dados SQL.",
                                        participantsToSave.size());

                        // Tutoring Ratings Data
                        // {mentorRatingFloat, tutoringSqlId, review}
                        Object[][] ratingData = {
                                        { 5.0f, 1L, "Excelente mentor! Muito claro e ajudou bastante na disciplina de Algoritmos." },
                                        { 4.0f, 2L, "Gostei da mentoria de Fundamentos de Computação, foi útil para entender a matéria." },
                                        { 3.0f, 3L, "Mentoria razoável sobre Laboratório de Programação, poderia ter mais exemplos." },
                                        { 5.0f, 4L, "Engenharia de Software ficou muito mais clara após esta sessão. Recomendo!" },
                                        { 4.0f, 5L, "Bom debate sobre Teologia e Ciências Exatas, o mentor conduziu bem." },
                                        { 5.0f, 6L, "Mentoria incrível sobre Engenharia de Requisitos, aprendi muito!" },
                                        { 4.0f, 7L, "O mentor de Fundamentos de POO explicou bem os conceitos chave." },
                                        { 3.0f, 8L, "Sessão ok sobre Fundamentos de Sistemas de Computação, cobriu o básico." },
                                        { 5.0f, 9L, "Estatística para IA parece menos intimidante agora. Ótima didática!" },
                                        { 4.0f, 10L, "Gostei da abordagem em Projeto de Banco de Dados, bem prático." },
                                        { 2.0f, 11L, "Esperava mais da mentoria de Segurança da Informação, achei um pouco vago." },
                                        { 5.0f, 12L, "DevOps e Gerência de Configuração explicados de forma exemplar!" },
                                        { 4.0f, 13L, "Boa explicação sobre Estrutura de Dados Orientada a Objeto." },
                                        { 5.0f, 14L, "IA Aplicada foi desmistificada! Mentor excelente!" },
                                        { 3.0f, 15L, "Desenvolvimento Web: mentoria regular, faltou aprofundamento em APIs." },
                                        { 4.0f, 16L, "Entendi melhor Mensageria e Streams, bom mentor." },
                                        { 5.0f, 17L, "Design de Software: sessão muito produtiva, com ótimos insights." },
                                        { 2.0f, 18L, "Gerência de Qualidade (EaD) não foi tão claro, achei confuso." },
                                        { 4.0f, 19L, "Modelagem de Interfaces de Usuário: bons exemplos práticos." },
                                        { 3.0f, 20L, "Ferramentas Visuais: ok, mas poderia mostrar mais ferramentas." },
                                        { 5.0f, 21L, "Desenvolvimento Mobile: o mentor domina o assunto, foi ótimo!" },
                                        { 4.0f, 22L, "Governança em TI (EaD): conteúdo bem apresentado." },
                                        { 5.0f, 23L, "POO com Banco de Dados: excelente, tirou todas as minhas dúvidas!" },
                                        { 3.0f, 24L, "Gerência de Projetos (EaD): um pouco teórico demais para mim." },
                                        { 4.0f, 25L, "Projeto Integrador: o mentor deu boas dicas de escopo." },
                                        { 5.0f, 26L, "Big Data e Ciência de Dados: introdução muito clara e interessante!" },
                                        { 4.0f, 27L, "Internet das Coisas: gostei dos exemplos de aplicação." },
                                        { 3.0f, 28L, "Negócios em TI (EAD): achei a discussão um pouco superficial." },
                                        { 4.0f, 29L, "Optativa I: a mentoria ajudou a entender as opções." },
                                        { 5.0f, 30L, "LIBRAS Instrumental: aprendi sinais importantes, ótimo!" },
                                        { 4.0f, 31L, "Interpretação de Texto: dicas úteis para provas." },
                                        { 5.0f, 32L, "Paradigmas de Linguagem: excelente explanação das diferenças!" },
                                        { 3.0f, 33L, "Probabilidade e Estatística: mentoria básica, mas ok." },
                                        { 4.0f, 34L, "Práticas de Desenvolvimento de Jogos: inspirador!" },
                                        { 5.0f, 35L, "Fundamentos de Jogos: o mentor é apaixonado pelo tema, contagiante!" },
                                        { 4.0f, 36L, "Banco de Dados II: bons exemplos de otimização de consultas." },
                                        { 3.0f, 37L, "Redes de Computadores I: poderia ser mais prático." },
                                        { 5.0f, 38L, "Mentor incrível para Algoritmos, muito paciente!" },
                                        { 4.0f, 39L, "Gostei da mentoria de Fund. Comp. I, bem didático." },
                                        { 3.0f, 40L, "Lab. Programação: esperava mais exercícios práticos." },
                                        { 5.0f, 41L, "Eng. de Software: o melhor mentor que já tive para esta matéria!" },
                                        { 4.0f, 42L, "Teologia e Ciências: uma perspectiva interessante." },
                                        { 5.0f, 43L, "Eng. de Requisitos: sessão muito valiosa, recomendo!" },
                                        { 4.0f, 44L, "Fund. POO: conceitos bem solidificados." },
                                        { 3.0f, 45L, "Fund. Sist. Comp.: mentoria ok, nada de excepcional." },
                                        { 5.0f, 46L, "Estatística para IA: abriu minha mente para a área!" },
                                        { 4.0f, 47L, "Projeto de BD: o mentor deu ótimas dicas de modelagem." },
                                        { 1.0f, 48L, "Segurança da Informação: não gostei, muito confuso." }, // Rating
                                                                                                              // 1.0f
                                                                                                              // instead
                                                                                                              // of 1
                                        { 5.0f, 49L, "DevOps: explicação clara e direta ao ponto!" },
                                        { 4.0f, 50L, "Estrutura de Dados: bom para revisar a matéria." },
                                        { 5.0f, 51L, "IA Aplicada: o mentor mostrou exemplos reais, foi ótimo!" },
                                        { 3.0f, 52L, "Desenvolvimento Web: esperava ver mais sobre frameworks." },
                                        { 4.0f, 53L, "Mensageria e Streams: conteúdo relevante e bem explicado." },
                                        { 5.0f, 54L, "Design de Software: mudou minha forma de pensar sobre arquitetura!" },
                                        { 1.0f, 55L, "Gerência de Qualidade: não foi o que eu esperava, infelizmente." }, // Rating
                                                                                                                          // 1.0f
                                        { 4.0f, 56L, "Modelagem de Interfaces: aprendi técnicas novas." },
                                        { 3.0f, 57L, "Ferramentas Visuais: apresentação básica das ferramentas." },
                                        { 5.0f, 58L, "Desenvolvimento Mobile: excelente, com muitos exemplos práticos!" },
                                        { 4.0f, 59L, "Governança em TI: o mentor simplificou os frameworks." },
                                        { 5.0f, 60L, "POO com BD: finalmente entendi a integração! Ótimo mentor." },
                                        { 3.0f, 61L, "Gerência de Projetos: um pouco repetitivo." },
                                        { 4.0f, 62L, "Projeto Integrador: ajudou a definir os próximos passos." },
                                        { 5.0f, 63L, "Big Data: Introdução fantástica ao tema!" },
                                        { 4.0f, 64L, "IoT: Explicações claras e exemplos interessantes." },
                                        { 3.0f, 65L, "Negócios em TI: Razoável, mas poderia ser mais aprofundado." },
                                        { 4.0f, 66L, "Optativa I: Boa discussão sobre as opções disponíveis." },
                                        { 5.0f, 67L, "LIBRAS: Muito bom, aprendi o básico para comunicação." },
                                        { 4.0f, 68L, "Interpretação de Texto: Dicas valiosas para o dia a dia." },
                                        { 5.0f, 69L, "Paradigmas de Linguagem: Mentor excelente, domina o assunto." },
                                        { 3.0f, 70L, "Probabilidade e Estatística: Achei a mentoria um pouco corrida." },
                                        { 4.0f, 71L, "Desenvolvimento de Jogos: Deu para ter uma boa noção da área." },
                                        { 5.0f, 72L, "Fundamentos de Jogos: Ótima introdução, bem empolgante!" },
                                        { 4.0f, 73L, "Banco de Dados II: Tirou dúvidas importantes sobre otimização." },
                                        { 3.0f, 74L, "Redes I: Esperava mais exemplos práticos de configuração." },
                                        { 5.0f, 75L, "Algoritmos: Revisão excelente dos principais tópicos." },
                                        { 4.0f, 76L, "Fund. Comp. I: Mentor paciente e com boa didática." },
                                        { 2.0f, 77L, "Lab. Programação: Não foi muito prático, mais teoria." },
                                        { 5.0f, 78L, "Eng. de Software: Uma das melhores mentorias que participei!" },
                                        { 4.0f, 79L, "Teologia e Ciências: Discussão muito rica e bem fundamentada." },
                                        { 5.0f, 80L, "Eng. de Requisitos: Essencial para quem quer seguir na área." },
                                        { 4.0f, 81L, "Fund. POO: Ajudou a fixar os pilares da POO." },
                                        { 3.0f, 82L, "Fund. Sist. Comp.: Mentoria ok, sem grandes novidades." },
                                        { 5.0f, 83L, "Estatística para IA: Ótimo para entender a base matemática." },
                                        { 4.0f, 84L, "Projeto de BD: Boas práticas de modelagem foram destacadas." },
                                        { 2.0f, 85L, "Segurança da Informação: Conteúdo um pouco desorganizado." },
                                        { 5.0f, 86L, "DevOps: Transformador! Abriu meus olhos para a cultura." },
                                        { 4.0f, 87L, "Estrutura de Dados: Revisão completa e bem feita." },
                                        { 5.0f, 88L, "IA Aplicada: Demonstrações práticas muito boas!" },
                                        { 3.0f, 89L, "Desenvolvimento Web: Faltou falar sobre testes e segurança." },
                                        { 4.0f, 90L, "Mensageria: Explicou bem a diferença entre as ferramentas." }
                        };

                        List<TutoringRating> ratingsToSave = new ArrayList<>();
                        for (Object[] data : ratingData) {
                                Tutoring tutoring = sqlTutoringIdToTutoringObjectMap.get((Long) data[1]);
                                if (tutoring == null) {
                                        log.warn("Tutoria (SQL ID: {}) não encontrada para avaliação. Pulando rating.",
                                                        data[1]);
                                        continue;
                                }

                                TutoringRating rating = TutoringRating.builder()
                                                .tutoring(tutoring)
                                                .mentorRating((Float) data[0])
                                                .review((String) data[2])
                                                .build();
                                // Associate rating with tutoring
                                tutoring.setRating(rating); // This should be handled by cascade if Tutoring is the
                                                            // owner or if we save tutoring later
                                ratingsToSave.add(rating);
                        }
                        tutoringRatingRepository.saveAll(ratingsToSave);
                        // userRepository.saveAll(usersToCreate); // users were already saved
                        // mentorAvailabilityRepository.saveAll(availabilitiesToCreate); //
                        // availabilities were already saved
                        tutoringRepository.saveAll(sqlTutoringIdToTutoringObjectMap.values()); // Ensure tutorings (with
                                                                                               // new ratings linked)
                                                                                               // are updated.

                        log.info(">>> {} avaliações de tutorias criadas com sucesso a partir dos dados SQL.",
                                        ratingsToSave.size());

                } else {
                        log.info(">>> Tutorias, participantes e/ou avaliações já existem ou mapas de usuário/disciplina estão vazios. Nenhuma ação necessária.");
                }
        }
}