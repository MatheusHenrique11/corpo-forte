-- Catalogo curado com conhecimento geral de calistenia (progressoes e
-- nomenclatura amplamente conhecidas), nao copiado de nenhum curso ou
-- material especifico. 54 exercicios: 6 padroes de movimento x 3 niveis x
-- 3 exercicios por combinacao.

insert into exercicio (nome, movimento, nivel, equipamento_necessario) values
-- PUXAR_VERTICAL
('Remada invertida em barra baixa', 'PUXAR_VERTICAL', 'INICIANTE', 'BARRA_FIXA'),
('Remada australiana em aneis', 'PUXAR_VERTICAL', 'INICIANTE', 'ANEIS'),
('Puxada assistida com elastico', 'PUXAR_VERTICAL', 'INICIANTE', 'ELASTICO'),
('Barra fixa completa', 'PUXAR_VERTICAL', 'INTERMEDIARIO', 'BARRA_FIXA'),
('Negativa lenta de barra fixa', 'PUXAR_VERTICAL', 'INTERMEDIARIO', 'BARRA_FIXA'),
('Puxada supinada (chin-up)', 'PUXAR_VERTICAL', 'INTERMEDIARIO', 'BARRA_FIXA'),
('Muscle-up', 'PUXAR_VERTICAL', 'AVANCADO', 'BARRA_FIXA'),
('Barra fixa arqueiro', 'PUXAR_VERTICAL', 'AVANCADO', 'BARRA_FIXA'),
('Barra fixa com peso adicional', 'PUXAR_VERTICAL', 'AVANCADO', 'BARRA_FIXA'),

-- EMPURRAR_VERTICAL
('Flexao pike no chao', 'EMPURRAR_VERTICAL', 'INICIANTE', 'NENHUM'),
('Flexao pike com pes elevados', 'EMPURRAR_VERTICAL', 'INICIANTE', 'NENHUM'),
('Apoio de ombros na parede', 'EMPURRAR_VERTICAL', 'INICIANTE', 'NENHUM'),
('Flexao pike profunda elevada', 'EMPURRAR_VERTICAL', 'INTERMEDIARIO', 'NENHUM'),
('Apoio invertido livre na parede', 'EMPURRAR_VERTICAL', 'INTERMEDIARIO', 'NENHUM'),
('Flexao pike em paralelas', 'EMPURRAR_VERTICAL', 'INTERMEDIARIO', 'PARALELAS'),
('Flexao com apoio invertido assistida na parede', 'EMPURRAR_VERTICAL', 'AVANCADO', 'NENHUM'),
('Apoio invertido livre sem parede', 'EMPURRAR_VERTICAL', 'AVANCADO', 'NENHUM'),
('Flexao com apoio invertido e deficit', 'EMPURRAR_VERTICAL', 'AVANCADO', 'PARALELAS'),

-- PERNAS_BILATERAL
('Agachamento livre', 'PERNAS_BILATERAL', 'INICIANTE', 'NENHUM'),
('Agachamento na parede (wall sit)', 'PERNAS_BILATERAL', 'INICIANTE', 'NENHUM'),
('Agachamento com apoio nas maos', 'PERNAS_BILATERAL', 'INICIANTE', 'NENHUM'),
('Agachamento tempo (descida controlada)', 'PERNAS_BILATERAL', 'INTERMEDIARIO', 'NENHUM'),
('Agachamento sumo', 'PERNAS_BILATERAL', 'INTERMEDIARIO', 'NENHUM'),
('Agachamento com salto', 'PERNAS_BILATERAL', 'INTERMEDIARIO', 'NENHUM'),
('Agachamento com salto em profundidade', 'PERNAS_BILATERAL', 'AVANCADO', 'BANCO'),
('Agachamento bulgaro', 'PERNAS_BILATERAL', 'AVANCADO', 'BANCO'),
('Agachamento com pausa profunda e salto', 'PERNAS_BILATERAL', 'AVANCADO', 'NENHUM'),

-- PERNAS_UNILATERAL
('Afundo estacionario', 'PERNAS_UNILATERAL', 'INICIANTE', 'NENHUM'),
('Step-up em banco baixo', 'PERNAS_UNILATERAL', 'INICIANTE', 'BANCO'),
('Agachamento bulgaro assistido', 'PERNAS_UNILATERAL', 'INICIANTE', 'BANCO'),
('Afundo com passada', 'PERNAS_UNILATERAL', 'INTERMEDIARIO', 'NENHUM'),
('Step-up em banco alto', 'PERNAS_UNILATERAL', 'INTERMEDIARIO', 'BANCO'),
('Pistol squat assistido', 'PERNAS_UNILATERAL', 'INTERMEDIARIO', 'NENHUM'),
('Pistol squat livre', 'PERNAS_UNILATERAL', 'AVANCADO', 'NENHUM'),
('Afundo com salto', 'PERNAS_UNILATERAL', 'AVANCADO', 'NENHUM'),
('Shrimp squat', 'PERNAS_UNILATERAL', 'AVANCADO', 'NENHUM'),

-- PUXAR_HORIZONTAL
('Remada horizontal inclinada em aneis', 'PUXAR_HORIZONTAL', 'INICIANTE', 'ANEIS'),
('Remada horizontal em barra baixa', 'PUXAR_HORIZONTAL', 'INICIANTE', 'BARRA_FIXA'),
('Remada com elastico', 'PUXAR_HORIZONTAL', 'INICIANTE', 'ELASTICO'),
('Remada horizontal corpo paralelo em aneis', 'PUXAR_HORIZONTAL', 'INTERMEDIARIO', 'ANEIS'),
('Remada arqueiro em aneis', 'PUXAR_HORIZONTAL', 'INTERMEDIARIO', 'ANEIS'),
('Remada com pausa no topo', 'PUXAR_HORIZONTAL', 'INTERMEDIARIO', 'ANEIS'),
('Remada em um braco assistida', 'PUXAR_HORIZONTAL', 'AVANCADO', 'ANEIS'),
('Front lever tuck (isometrico)', 'PUXAR_HORIZONTAL', 'AVANCADO', 'BARRA_FIXA'),
('Remada explosiva em aneis', 'PUXAR_HORIZONTAL', 'AVANCADO', 'ANEIS'),

-- EMPURRAR_HORIZONTAL
('Flexao inclinada (maos elevadas)', 'EMPURRAR_HORIZONTAL', 'INICIANTE', 'BANCO'),
('Flexao de joelhos', 'EMPURRAR_HORIZONTAL', 'INICIANTE', 'NENHUM'),
('Flexao negativa', 'EMPURRAR_HORIZONTAL', 'INICIANTE', 'NENHUM'),
('Flexao completa', 'EMPURRAR_HORIZONTAL', 'INTERMEDIARIO', 'NENHUM'),
('Flexao com pes elevados', 'EMPURRAR_HORIZONTAL', 'INTERMEDIARIO', 'BANCO'),
('Flexao diamante', 'EMPURRAR_HORIZONTAL', 'INTERMEDIARIO', 'NENHUM'),
('Flexao arqueiro', 'EMPURRAR_HORIZONTAL', 'AVANCADO', 'NENHUM'),
('Flexao com um braco assistida', 'EMPURRAR_HORIZONTAL', 'AVANCADO', 'NENHUM'),
('Flexao pseudo planche', 'EMPURRAR_HORIZONTAL', 'AVANCADO', 'PARALELAS');
