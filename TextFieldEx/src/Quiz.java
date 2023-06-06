import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

public class Quiz extends JFrame {
    private static final int TIME_LIMIT = 30; // 시험 제한 시간 (초)

    private Connection connection;
    private Statement statement;
    private JLabel questionLabel;
    private JTextField answerField;
    private JButton nextButton;
    private JButton addButton; // 문제 등록 버튼
    private JButton viewRankingButton; // 랭킹 보기 버튼
    private JLabel timerLabel;
    private Timer timer;
    private int questionIndex;
    private int score;
    private int remainingTime;

    public Quiz() throws SQLException {
        // 데이터베이스 연결 생성
        connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/java", "root", "");

        // 명령문 생성
        statement = connection.createStatement();

        // UI 초기화
        setTitle("Exam");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel(new GridBagLayout());
        setContentPane(contentPane);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(10, 10, 10, 10);

        questionLabel = new JLabel();
        contentPane.add(questionLabel, c);

        c.gridy = 1;
        answerField = new JTextField(20);
        contentPane.add(answerField, c);

        c.gridy = 2;
        nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processAnswer();
            }
        });
        contentPane.add(nextButton, c);

        c.gridx = 1;
        c.gridy = 0;
        addButton = new JButton("문제 등록");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseTimer(); // 타이머 일시정지
                showAddQuestionDialog(); // 문제 등록 다이얼로그 표시
            }
        });
        contentPane.add(addButton, c); // 문제 등록 버튼 추가

        c.gridy = 1;
        viewRankingButton = new JButton("랭킹 보기");
        viewRankingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewRanking();
            }
        });
        contentPane.add(viewRankingButton, c); // 랭킹 보기 버튼 추가

        c.gridy = 2;
        timerLabel = new JLabel();
        contentPane.add(timerLabel, c);

        // 첫 번째 문제 출력
        questionIndex = 1;
        showQuestion(questionIndex);

        // 타이머 설정
        remainingTime = TIME_LIMIT;
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTimer();
            }
        });
        timer.start();

        // UI 크기 조정
        pack();
        setLocationRelativeTo(null); // 화면 가운데에 표시

        // UI 표시
        setVisible(true);
    }

    private void showQuestion(int index) {
        // 데이터베이스에서 문제 조회
        try {
            ResultSet resultSet = statement.executeQuery("SELECT question FROM exam WHERE questionIndex = " + index);
            if (resultSet.next()) {
                String question = resultSet.getString("question");
                questionLabel.setText(question);
            } else {
                processAnswer();
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void viewRanking() {
        try {
            ResultSet resultSet = statement.executeQuery("SELECT username, score FROM ranking ORDER BY score DESC");
            StringBuilder rankingBuilder = new StringBuilder();
            rankingBuilder.append("랭킹:\n");
            rankingBuilder.append("--------------------------------\n");
            int rank = 1;
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                int score = resultSet.getInt("score");
                rankingBuilder.append("순위 ").append(rank).append(": ").append(username).append(" (").append(score).append(")\n");
                rank++;
            }
            JOptionPane.showMessageDialog(this, rankingBuilder.toString(), "랭킹", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "랭킹을 가져오는 중에 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processAnswer() {
        // 답변 처리
        String answer = answerField.getText();

        // 데이터베이스에서 정답 조회
        try {
            ResultSet resultSet = statement.executeQuery("SELECT answer FROM exam WHERE questionIndex = " + questionIndex);
            if (resultSet.next()) {
                String correctAnswer = resultSet.getString("answer");
                if (answer.equals(correctAnswer)) {
                    score++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 다음 문제로 이동 또는 결과 출력
        if (questionIndex < 5) {
            questionIndex++;
            showQuestion(questionIndex);
            answerField.setText("");
        } else {
            saveScore(); // 점수 저장
            showResult();
        }
    }

    private void updateTimer() {
        remainingTime--;
        timerLabel.setText("Time: " + remainingTime + "s");

        if (remainingTime <= 0) {
            timer.stop();
            saveScore(); // 점수 저장
            showResult();
        }
    }

    private void saveScore() {
        String username = JOptionPane.showInputDialog(this, "사용자 이름을 입력하세요:", "저장", JOptionPane.PLAIN_MESSAGE);
        if (username != null && !username.isEmpty()) {
            try {
                String query = "INSERT INTO ranking (username, score) VALUES (?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, username);
                preparedStatement.setInt(2, score);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "점수를 저장하는 중에 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }



    private void showResult() {
        // 등급 부여
        String grade;
        if (score == 5) {
            grade = "A";
        } else if (score == 4) {
            grade = "B";
        } else if (score == 3) {
            grade = "C";
        } else if (score == 2) {
            grade = "D";
        } else {
            grade = "F";
        }

        // 결과 출력
        JOptionPane.showMessageDialog(this, "점수: " + score + "\n등급: " + grade, "시험 결과",
                JOptionPane.INFORMATION_MESSAGE);

        // 데이터베이스 연결 닫기
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 프로그램 종료
        System.exit(0);
    }

    private void showAddQuestionDialog() {
        JDialog dialog = new JDialog(this, "문제 등록", Dialog.ModalityType.APPLICATION_MODAL);

        JLabel questionLabel = new JLabel("문제:");
        JTextField questionField = new JTextField(20);
        
        JLabel answerLabel = new JLabel("정답:");
        JTextField answerField = new JTextField(20);
        
        JButton addButton = new JButton("추가");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String question = questionField.getText();
                String answer = answerField.getText();
                
                if (!question.isEmpty() && !answer.isEmpty()) {
                    try {
                        // 데이터베이스에 문제와 정답 추가
                        String query = "INSERT INTO exam (question, answer) VALUES (?, ?)";
                        PreparedStatement preparedStatement = connection.prepareStatement(query);
                        preparedStatement.setString(1, question);
                        preparedStatement.setString(2, answer);
                        preparedStatement.executeUpdate();

                        JOptionPane.showMessageDialog(dialog, "문제가 성공적으로 등록되었습니다.", "문제 등록",
                                JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose(); // 다이얼로그 닫기
                        resumeTimer(); // 타이머 다시 시작
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(dialog, "문제 등록에 실패했습니다.", "문제 등록", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(dialog, "문제와 정답을 입력하세요.", "문제 등록", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        JPanel panel = new JPanel();
        panel.add(questionLabel);
        panel.add(questionField);
        panel.add(answerLabel);
        panel.add(answerField);
        panel.add(addButton);
        dialog.add(panel);

        dialog.pack();
        dialog.setLocationRelativeTo(this); // 부모 프레임 중앙에 표시

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // 다이얼로그 닫힐 때 작업 설정

        // 다이얼로그 닫히기 전에 타이머 일시정지
        pauseTimer();

        // 다이얼로그가 닫히면 타이머 다시 시작
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                resumeTimer();
            }
        });

        dialog.setVisible(true);
    }

    private void pauseTimer() {
        timer.stop();
    }

    private void resumeTimer() {
        timer.start();
    }

    public static void main(String[] args) {
        try {
            new Quiz();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "데이터베이스 연결에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}
