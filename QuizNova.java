package com.quiznova;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizNova extends JFrame {
    private int loggedStudentId = -1;
    private String loggedStudentName = "";

    public QuizNova() {
        setTitle("QuizNova - Online Quiz Management System");
        setSize(850, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel heading = new JLabel("QuizNova", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 30));
        heading.setBorder(new EmptyBorder(18, 10, 18, 10));
        add(heading, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Student Login", studentLoginPanel());
        tabs.addTab("Registration", registrationPanel());
        tabs.addTab("Admin", adminLoginPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel studentLoginPanel() {
        JPanel p = formPanel();
        JTextField email = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        addField(p, "Email", email);
        addField(p, "Password", password);
        JButton login = new JButton("Login");
        login.addActionListener(e -> {
            String sql = "SELECT student_id, name FROM students WHERE email=? AND password=?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email.getText().trim());
                ps.setString(2, new String(password.getPassword()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        loggedStudentId = rs.getInt("student_id");
                        loggedStudentName = rs.getString("name");
                        openStudentDashboard();
                    } else {
                        message("Invalid email or password.");
                    }
                }
            } catch (SQLException ex) { error(ex); }
        });
        p.add(login);
        return p;
    }

    private JPanel registrationPanel() {
        JPanel p = formPanel();
        JTextField name = new JTextField(22);
        JTextField email = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        addField(p, "Full name", name);
        addField(p, "Email", email);
        addField(p, "Password", password);
        JButton register = new JButton("Create Account");
        register.addActionListener(e -> {
            if (name.getText().isBlank() || email.getText().isBlank()
                    || password.getPassword().length == 0) {
                message("Please fill in all fields.");
                return;
            }
            String sql = "INSERT INTO students(name,email,password) VALUES(?,?,?)";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name.getText().trim());
                ps.setString(2, email.getText().trim());
                ps.setString(3, new String(password.getPassword()));
                ps.executeUpdate();
                message("Registration successful. You can now log in.");
                name.setText(""); email.setText(""); password.setText("");
            } catch (SQLException ex) { error(ex); }
        });
        p.add(register);
        return p;
    }

    private JPanel adminLoginPanel() {
        JPanel p = formPanel();
        JTextField username = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        addField(p, "Admin username", username);
        addField(p, "Admin password", password);
        JButton login = new JButton("Admin Login");
        login.addActionListener(e -> {
            String sql = "SELECT admin_id FROM admins WHERE username=? AND password=?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username.getText().trim());
                ps.setString(2, new String(password.getPassword()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) openAdminDashboard();
                    else message("Invalid admin credentials.");
                }
            } catch (SQLException ex) { error(ex); }
        });
        p.add(login);
        JLabel hint = new JLabel("Demo login: admin / admin123");
        hint.setBorder(new EmptyBorder(12, 0, 0, 0));
        p.add(hint);
        return p;
    }

    private void openStudentDashboard() {
        JFrame frame = new JFrame("QuizNova - Student Dashboard");
        frame.setSize(750, 520);
        frame.setLocationRelativeTo(this);
        frame.setLayout(new BorderLayout(10, 10));
        JLabel welcome = new JLabel("Welcome, " + loggedStudentName, SwingConstants.CENTER);
        welcome.setFont(new Font("Arial", Font.BOLD, 22));
        frame.add(welcome, BorderLayout.NORTH);

        DefaultListModel<QuizItem> model = new DefaultListModel<>();
        JList<QuizItem> quizList = new JList<>(model);
        loadQuizzes(model);
        frame.add(new JScrollPane(quizList), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton start = new JButton("Start Selected Quiz");
        JButton results = new JButton("My Results");
        start.addActionListener(e -> {
            QuizItem item = quizList.getSelectedValue();
            if (item == null) { message("Select a quiz first."); return; }
            frame.dispose();
            startQuiz(item);
        });
        results.addActionListener(e -> showStudentResults());
        bottom.add(start); bottom.add(results);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void loadQuizzes(DefaultListModel<QuizItem> model) {
        model.clear();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT quiz_id,quiz_name,subject FROM quizzes ORDER BY quiz_id")) {
            while (rs.next()) model.addElement(new QuizItem(
                    rs.getInt("quiz_id"), rs.getString("quiz_name"), rs.getString("subject")));
        } catch (SQLException ex) { error(ex); }
    }

    private void startQuiz(QuizItem quiz) {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE quiz_id=? ORDER BY question_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quiz.id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(new Question(rs.getInt("question_id"),
                            rs.getString("question_text"),
                            new String[]{rs.getString("option1"), rs.getString("option2"),
                                    rs.getString("option3"), rs.getString("option4")},
                            rs.getString("correct_answer")));
                }
            }
        } catch (SQLException ex) { error(ex); return; }

        if (questions.isEmpty()) { message("This quiz has no questions yet."); openStudentDashboard(); return; }

        JFrame frame = new JFrame("Quiz: " + quiz.name);
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(this);
        frame.setLayout(new BorderLayout(10, 10));
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        List<ButtonGroup> groups = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            JPanel card = new JPanel(new GridLayout(0, 1, 4, 4));
            card.setBorder(BorderFactory.createTitledBorder((i + 1) + ". " + q.text));
            ButtonGroup group = new ButtonGroup();
            for (String option : q.options) {
                JRadioButton rb = new JRadioButton(option);
                group.add(rb);
                card.add(rb);
            }
            groups.add(group);
            list.add(card);
        }

        frame.add(new JScrollPane(list), BorderLayout.CENTER);
        JButton submit = new JButton("Submit Quiz");
        submit.addActionListener(e -> {
            int score = 0;
            for (int i = 0; i < questions.size(); i++) {
                for (AbstractButton b : java.util.Collections.list(groups.get(i).getElements())) {
                    if (b.isSelected() && b.getText().equals(questions.get(i).answer)) score++;
                }
            }
            String save = "INSERT INTO results(student_id,quiz_id,score,total_questions) VALUES(?,?,?,?)";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(save)) {
                ps.setInt(1, loggedStudentId);
                ps.setInt(2, quiz.id);
                ps.setInt(3, score);
                ps.setInt(4, questions.size());
                ps.executeUpdate();
                frame.dispose();
                JOptionPane.showMessageDialog(this, "Quiz submitted!\nYour score: "
                        + score + " / " + questions.size());
                openStudentDashboard();
            } catch (SQLException ex) { error(ex); }
        });
        JPanel bottom = new JPanel();
        bottom.add(submit);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void showStudentResults() {
        String sql = "SELECT q.quiz_name,r.score,r.total_questions,r.attempted_at " +
                "FROM results r JOIN quizzes q ON r.quiz_id=q.quiz_id " +
                "WHERE r.student_id=? ORDER BY r.attempted_at DESC";
        StringBuilder out = new StringBuilder("Quiz                         Score       Date\n\n");
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, loggedStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.append(rs.getString(1)).append("     ")
                        .append(rs.getInt(2)).append("/").append(rs.getInt(3))
                        .append("     ").append(rs.getTimestamp(4)).append("\n");
            }
            JOptionPane.showMessageDialog(this, out.toString(), "My Results",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) { error(ex); }
    }

    private void openAdminDashboard() {
        JFrame frame = new JFrame("QuizNova - Admin Dashboard");
        frame.setSize(850, 600);
        frame.setLocationRelativeTo(this);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Add Quiz", addQuizPanel());
        tabs.addTab("Add Question", addQuestionPanel());
        tabs.addTab("Manage Questions", manageQuestionsPanel());
        tabs.addTab("All Results", allResultsPanel());
        frame.add(tabs);
        frame.setVisible(true);
    }

    private JPanel addQuizPanel() {
        JPanel p = formPanel();
        JTextField name = new JTextField(25), subject = new JTextField(25);
        addField(p, "Quiz name", name);
        addField(p, "Subject", subject);
        JButton add = new JButton("Add Quiz");
        add.addActionListener(e -> {
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "INSERT INTO quizzes(quiz_name,subject) VALUES(?,?)")) {
                ps.setString(1, name.getText().trim());
                ps.setString(2, subject.getText().trim());
                ps.executeUpdate();
                message("Quiz added successfully.");
                name.setText(""); subject.setText("");
            } catch (SQLException ex) { error(ex); }
        });
        p.add(add);
        return p;
    }

    private Component addQuestionPanel() {
        JPanel p = formPanel();
        JComboBox<QuizItem> quiz = new JComboBox<>();
        loadQuizCombo(quiz);
        JTextField question = new JTextField(30);
        JTextField[] options = {new JTextField(25), new JTextField(25),
                new JTextField(25), new JTextField(25)};
        JTextField answer = new JTextField(25);
        addField(p, "Select quiz", quiz);
        addField(p, "Question", question);
        for (int i = 0; i < 4; i++) addField(p, "Option " + (i + 1), options[i]);
        addField(p, "Correct answer (type exact option)", answer);
        JButton add = new JButton("Save Question");
        add.addActionListener(e -> {
            QuizItem selected = (QuizItem) quiz.getSelectedItem();
            if (selected == null) { message("Create a quiz first."); return; }
            boolean valid = !question.getText().isBlank() && !answer.getText().isBlank();
            for (JTextField f : options) valid &= !f.getText().isBlank();
            boolean answerExists = false;
            for (JTextField f : options) if (f.getText().trim().equals(answer.getText().trim())) answerExists = true;
            if (!valid || !answerExists) { message("Fill all fields. Correct answer must match one option exactly."); return; }
            String sql = "INSERT INTO questions(quiz_id,question_text,option1,option2,option3,option4,correct_answer) VALUES(?,?,?,?,?,?,?)";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, selected.id);
                ps.setString(2, question.getText().trim());
                for (int i = 0; i < 4; i++) ps.setString(i + 3, options[i].getText().trim());
                ps.setString(7, answer.getText().trim());
                ps.executeUpdate();
                message("Question saved.");
                question.setText(""); for (JTextField f : options) f.setText(""); answer.setText("");
            } catch (SQLException ex) { error(ex); }
        });
        p.add(add);
        return new JScrollPane(p);
    }

    private JPanel manageQuestionsPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        JComboBox<QuizItem> quiz = new JComboBox<>();
        loadQuizCombo(quiz);
        JButton refresh = new JButton("Refresh");
        JButton delete = new JButton("Delete Selected Question");
        Runnable load = () -> {
            model.clear();
            QuizItem item = (QuizItem) quiz.getSelectedItem();
            if (item == null) return;
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT question_id,question_text FROM questions WHERE quiz_id=? ORDER BY question_id")) {
                ps.setInt(1, item.id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) model.addElement(rs.getInt(1) + " | " + rs.getString(2));
                }
            } catch (SQLException ex) { error(ex); }
        };
        refresh.addActionListener(e -> load.run());
        quiz.addActionListener(e -> load.run());
        delete.addActionListener(e -> {
            String selected = list.getSelectedValue();
            if (selected == null) { message("Select a question."); return; }
            int id = Integer.parseInt(selected.substring(0, selected.indexOf(" |")));
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM questions WHERE question_id=?")) {
                ps.setInt(1, id); ps.executeUpdate(); load.run(); message("Question deleted.");
            } catch (SQLException ex) { error(ex); }
        });
        JPanel top = new JPanel(); top.add(new JLabel("Quiz:")); top.add(quiz); top.add(refresh);
        JPanel bottom = new JPanel(); bottom.add(delete);
        p.add(top, BorderLayout.NORTH); p.add(new JScrollPane(list), BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel allResultsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JTextArea area = new JTextArea();
        area.setEditable(false);
        JButton refresh = new JButton("Load Results");
        refresh.addActionListener(e -> {
            area.setText("Student | Email | Quiz | Score | Attempted At\n\n");
            String sql = "SELECT s.name,s.email,q.quiz_name,r.score,r.total_questions,r.attempted_at " +
                    "FROM results r JOIN students s ON r.student_id=s.student_id " +
                    "JOIN quizzes q ON r.quiz_id=q.quiz_id ORDER BY r.attempted_at DESC";
            try (Connection con = DBConnection.getConnection();
                 Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) area.append(rs.getString(1) + " | " + rs.getString(2)
                        + " | " + rs.getString(3) + " | " + rs.getInt(4) + "/"
                        + rs.getInt(5) + " | " + rs.getTimestamp(6) + "\n");
            } catch (SQLException ex) { error(ex); }
        });
        p.add(refresh, BorderLayout.NORTH); p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }

    private void loadQuizCombo(JComboBox<QuizItem> combo) {
        combo.removeAllItems();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT quiz_id,quiz_name,subject FROM quizzes ORDER BY quiz_id")) {
            while (rs.next()) combo.addItem(new QuizItem(rs.getInt(1), rs.getString(2), rs.getString(3)));
        } catch (SQLException ex) { error(ex); }
    }

    private JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(25, 35, 25, 35));
        return p;
    }

    private void addField(JPanel p, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 7, 7, 7);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = p.getComponentCount();
        p.add(new JLabel(label), c);
        c.gridx = 1; p.add(field, c);
    }

    private void message(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class QuizItem {
        int id; String name; String subject;
        QuizItem(int id, String name, String subject) {
            this.id = id; this.name = name; this.subject = subject;
        }
        public String toString() { return name + " (" + subject + ")"; }
    }

    private static class Question {
        int id; String text; String[] options; String answer;
        Question(int id, String text, String[] options, String answer) {
            this.id = id; this.text = text; this.options = options; this.answer = answer;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizNova().setVisible(true));
    }
}
