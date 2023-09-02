eval $(minikube docker-env)
docker build -t firevox/main:latest ./main
docker build -t firevox/worker:latest ./worker