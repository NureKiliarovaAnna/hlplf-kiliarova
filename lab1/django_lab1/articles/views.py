from django.shortcuts import render, get_object_or_404
from django.contrib.auth.models import User
from .models import Article
from rest_framework.decorators import api_view
from rest_framework.response import Response
from .serializers import ArticleSerializer


@api_view(['GET'])
def article_api_list(request):
    articles = Article.objects.all()
    serializer = ArticleSerializer(articles, many=True)

    return Response(serializer.data)

def article_list(request):
    articles = Article.objects.all()

    return render(request, 'articles/article_list.html', {
        'articles': articles
    })


def article_detail(request, pk):
    article = get_object_or_404(Article, pk=pk)

    return render(request, 'articles/article_detail.html', {
        'article': article
    })


def articles_by_author(request, author_id):
    author = get_object_or_404(User, pk=author_id)
    articles = Article.objects.filter(author=author)

    return render(request, 'articles/articles_by_author.html', {
        'author': author,
        'articles': articles
    })