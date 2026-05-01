from rest_framework import serializers
from .models import Article


class ArticleSerializer(serializers.ModelSerializer):
    author = serializers.StringRelatedField()

    class Meta:
        model = Article
        fields = ['id', 'title', 'text', 'publication_date', 'author']